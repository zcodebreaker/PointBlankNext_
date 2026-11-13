package com.pointblank.api.webstore;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;
import com.pointblank.api.webstore.WebstoreRepository.PurchaseDelivery;
import com.pointblank.api.webstore.WebstoreRepository.PurchaseItem;

@Service
public class WebstoreService {

    private static final int ADMIN_ACCESS_LEVEL = 5;
    private static final long MAX_COVER_BYTES = 2L * 1024 * 1024;
    private static final Set<String> COVER_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final WebstoreRepository webstoreRepository;
    private final AccountRepository accountRepository;

    public WebstoreService(WebstoreRepository webstoreRepository, AccountRepository accountRepository) {
        this.webstoreRepository = webstoreRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<WebstoreItem> findCatalog() {
        return webstoreRepository.findCatalog();
    }

    @Transactional(readOnly = true)
    public List<WebstoreHistoryItem> findPurchaseHistory(long playerId, int months) {
        if (!Set.of(1, 3, 6).contains(months)) {
            throw new WebstoreException(HttpStatus.BAD_REQUEST, "O período deve ser de 1, 3 ou 6 meses");
        }
        return webstoreRepository.findPurchaseHistory(playerId, OffsetDateTime.now().minusMonths(months));
    }

    @Transactional(readOnly = true)
    public List<Integer> findPurchasedItemIds(long playerId) {
        return webstoreRepository.findPurchasedItemIds(playerId);
    }

    @Transactional(readOnly = true)
    public List<AdminWebstoreItem> findAdminCatalog(long administratorId) {
        requireAdministrator(administratorId);
        return webstoreRepository.findAdminCatalog();
    }

    @Transactional
    public AdminWebstoreItem saveCatalogItem(
            long administratorId,
            int goodId,
            int priceCash,
            String name,
            MultipartFile coverFile) {
        requireAdministrator(administratorId);
        if (goodId <= 0) {
            throw new WebstoreException(HttpStatus.BAD_REQUEST, "Informe um good_id válido");
        }
        webstoreRepository.findShopOffer(goodId)
                .orElseThrow(() -> new WebstoreException(
                        HttpStatus.NOT_FOUND,
                        "O good_id informado não existe na tabela shop"));
        if (priceCash <= 0) {
            throw new WebstoreException(HttpStatus.BAD_REQUEST, "Informe um valor em Cash maior que zero");
        }

        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty() || normalizedName.length() > 255) {
            throw new WebstoreException(HttpStatus.BAD_REQUEST, "Informe um nome entre 1 e 255 caracteres");
        }

        WebstoreCover cover = validateCover(coverFile);
        String filename = coverFile.getOriginalFilename() == null
                ? "cover"
                : coverFile.getOriginalFilename().trim();
        if (!webstoreRepository.updateShopOfferPrice(goodId, priceCash)) {
            throw new WebstoreException(HttpStatus.NOT_FOUND, "A oferta não pôde ser atualizada na tabela shop");
        }
        webstoreRepository.saveCatalogItem(
                goodId,
                normalizedName,
                filename,
                cover.contentType(),
                cover.data());
        return webstoreRepository.findAdminCatalogItem(goodId)
                .orElseThrow(() -> new IllegalStateException("O produto salvo não pôde ser carregado"));
    }

    @Transactional(readOnly = true)
    public WebstoreCover findCover(int goodId) {
        return webstoreRepository.findCover(goodId)
                .orElseThrow(() -> new WebstoreException(HttpStatus.NOT_FOUND, "Capa do produto não encontrada"));
    }

    @Transactional
    public WebstorePurchaseResponse purchase(long playerId, WebstorePurchaseRequest request) {
        var previousPurchase = webstoreRepository.findPurchase(playerId, request.requestId());
        if (previousPurchase.isPresent()) return previousPurchase.get();

        Account account = accountRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new WebstoreException(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
        if (account.banned()) {
            throw new WebstoreException(HttpStatus.FORBIDDEN, "Contas banidas não podem realizar compras");
        }

        PurchaseItem item = webstoreRepository.findPurchasableByGoodId(request.goodId())
                .orElseThrow(() -> new WebstoreException(HttpStatus.NOT_FOUND, "Produto indisponível"));
        previousPurchase = webstoreRepository.findPurchase(playerId, request.requestId());
        if (previousPurchase.isPresent()) return previousPurchase.get();

        if (!webstoreRepository.reservePurchaseEntitlement(playerId, item.itemId())) {
            previousPurchase = webstoreRepository.findPurchase(playerId, request.requestId());
            if (previousPurchase.isPresent()) return previousPurchase.get();
            throw new WebstoreException(HttpStatus.CONFLICT, "Este item já foi adquirido por esta conta");
        }

        if (account.cash() < item.priceCash()) {
            throw new WebstoreException(HttpStatus.CONFLICT, "Cash insuficiente para concluir a compra");
        }

        int cashAfter = webstoreRepository.deductCash(playerId, item.priceCash())
                .orElseThrow(() -> new WebstoreException(HttpStatus.CONFLICT, "O saldo mudou. Atualize a loja e tente novamente"));
        long deliveredObjectId = webstoreRepository.deliverItem(playerId, item);
        WebstorePurchaseResponse response = webstoreRepository.recordPurchase(
                request.requestId(), playerId, item, cashAfter, deliveredObjectId);
        webstoreRepository.linkPurchaseEntitlement(playerId, item.itemId(), response.purchaseId());
        return response;
    }

    @Transactional
    public WebstoreDeliveryStatus verifyDelivery(long playerId, long purchaseId) {
        PurchaseDelivery purchase = requirePurchaseDelivery(playerId, purchaseId);
        if (resolveDelivery(playerId, purchase)) {
            return deliveryStatus(purchase, true, "O item está disponível no inventário da conta.");
        }
        return deliveryStatus(purchase, false, purchase.recoveryCount() == 0
                ? "O item não foi localizado. Você pode resgatá-lo novamente uma vez."
                : "O item não foi localizado e o re-resgate desta compra já foi utilizado.");
    }

    @Transactional
    public WebstoreDeliveryStatus redeemDelivery(long playerId, long purchaseId) {
        PurchaseDelivery purchase = requirePurchaseDelivery(playerId, purchaseId);
        if (resolveDelivery(playerId, purchase)) {
            return deliveryStatus(purchase, true, "O item já está disponível no inventário da conta.");
        }
        if (purchase.recoveryCount() >= 1) {
            throw new WebstoreException(HttpStatus.CONFLICT, "O re-resgate desta compra já foi utilizado");
        }

        Account account = accountRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new WebstoreException(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
        if (account.banned()) {
            throw new WebstoreException(HttpStatus.FORBIDDEN, "Contas banidas não podem resgatar itens");
        }

        PurchaseItem item = new PurchaseItem(
                purchase.goodId(), purchase.itemId(), purchase.itemName(), purchase.priceCash(), purchase.itemCount());
        long deliveredObjectId = webstoreRepository.deliverItem(playerId, item);
        webstoreRepository.recordRecovery(purchase.purchaseId(), deliveredObjectId);
        return new WebstoreDeliveryStatus(
                purchase.purchaseId(), true, false, 1, "Item resgatado e entregue novamente ao inventário.");
    }

    private PurchaseDelivery requirePurchaseDelivery(long playerId, long purchaseId) {
        if (purchaseId <= 0) {
            throw new WebstoreException(HttpStatus.BAD_REQUEST, "Compra inválida");
        }
        return webstoreRepository.findPurchaseDeliveryForUpdate(playerId, purchaseId)
                .orElseThrow(() -> new WebstoreException(HttpStatus.NOT_FOUND, "Compra não encontrada"));
    }

    private boolean resolveDelivery(long playerId, PurchaseDelivery purchase) {
        if (purchase.deliveredObjectId() != null) {
            return webstoreRepository.deliveryExists(playerId, purchase.deliveredObjectId(), purchase.itemId());
        }

        Optional<Long> legacyDelivery = webstoreRepository.findUnlinkedDeliveryObject(playerId, purchase.itemId());
        if (legacyDelivery.isEmpty()) return false;
        webstoreRepository.linkDelivery(purchase.purchaseId(), legacyDelivery.get());
        return true;
    }

    private WebstoreDeliveryStatus deliveryStatus(PurchaseDelivery purchase, boolean delivered, String message) {
        return new WebstoreDeliveryStatus(
                purchase.purchaseId(), delivered, !delivered && purchase.recoveryCount() == 0,
                purchase.recoveryCount(), message);
    }

    private void requireAdministrator(long administratorId) {
        Account account = accountRepository.findByPlayerId(administratorId)
                .orElseThrow(() -> new WebstoreException(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
        if (account.accessLevel() < ADMIN_ACCESS_LEVEL) {
            throw new WebstoreException(HttpStatus.FORBIDDEN, "Acesso administrativo necessário");
        }
    }

    private WebstoreCover validateCover(MultipartFile coverFile) {
        if (coverFile == null || coverFile.isEmpty()) {
            throw new WebstoreException(HttpStatus.BAD_REQUEST, "Selecione uma capa para o produto");
        }
        if (coverFile.getSize() > MAX_COVER_BYTES) {
            throw new WebstoreException(HttpStatus.BAD_REQUEST, "A capa deve possuir no máximo 2 MB");
        }

        String contentType = coverFile.getContentType() == null
                ? ""
                : coverFile.getContentType().toLowerCase(Locale.ROOT);
        if (!COVER_CONTENT_TYPES.contains(contentType)) {
            throw new WebstoreException(HttpStatus.BAD_REQUEST, "A capa deve ser uma imagem JPG ou PNG");
        }

        try {
            byte[] data = coverFile.getBytes();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                throw new WebstoreException(HttpStatus.BAD_REQUEST, "O arquivo enviado não é uma imagem válida");
            }
            return new WebstoreCover(data, contentType);
        } catch (IOException exception) {
            throw new WebstoreException(HttpStatus.BAD_REQUEST, "Não foi possível ler a capa enviada");
        }
    }
}
