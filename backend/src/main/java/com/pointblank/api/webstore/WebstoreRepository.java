package com.pointblank.api.webstore;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class WebstoreRepository {

    private final JdbcClient jdbcClient;

    public WebstoreRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<WebstoreItem> findCatalog() {
        return jdbcClient.sql("""
                SELECT s.good_id, s.item_id,
                       COALESCE(NULLIF(c.display_name, ''), s.item_name) AS item_name,
                       s.price_cash, s.count, s.tag,
                       CASE WHEN c.cover_data IS NOT NULL
                            THEN '/api/v1/webstore/items/' || s.good_id || '/cover'
                            ELSE c.image_path
                       END AS image_path
                FROM public.webstore_catalog c
                JOIN public.shop s ON s.good_id = c.good_id
                WHERE c.active = TRUE
                  AND s.visibility = 4
                  AND s.price_cash > 0
                ORDER BY c.sort_order, s.good_id
                """)
                .query((resultSet, rowNum) -> mapItem(
                        resultSet.getInt("good_id"),
                        resultSet.getInt("item_id"),
                        resultSet.getString("item_name"),
                        resultSet.getInt("price_cash"),
                        resultSet.getLong("count"),
                        resultSet.getInt("tag"),
                        resultSet.getString("image_path")))
                .list();
    }

    public Optional<PurchaseItem> findPurchasableByGoodId(int goodId) {
        return jdbcClient.sql("""
                SELECT s.good_id, s.item_id,
                       COALESCE(NULLIF(c.display_name, ''), s.item_name) AS item_name,
                       s.price_cash, s.count
                FROM public.webstore_catalog c
                JOIN public.shop s ON s.good_id = c.good_id
                WHERE c.active = TRUE
                  AND s.visibility = 4
                  AND s.price_cash > 0
                  AND s.good_id = :goodId
                FOR UPDATE OF s
                """)
                .param("goodId", goodId)
                .query((resultSet, rowNum) -> new PurchaseItem(
                        resultSet.getInt("good_id"),
                        resultSet.getInt("item_id"),
                        normalizedName(resultSet.getString("item_name")),
                        resultSet.getInt("price_cash"),
                        resultSet.getLong("count")))
                .optional();
    }

    public List<AdminWebstoreItem> findAdminCatalog() {
        return jdbcClient.sql("""
                SELECT s.good_id, s.item_id,
                       COALESCE(NULLIF(c.display_name, ''), s.item_name) AS item_name,
                       s.price_cash, s.count,
                       CASE WHEN c.cover_data IS NOT NULL
                            THEN '/api/v1/webstore/items/' || s.good_id || '/cover'
                            ELSE c.image_path
                       END AS image_path,
                       c.active, c.sort_order, (c.cover_data IS NOT NULL) AS custom_cover
                FROM public.webstore_catalog c
                JOIN public.shop s ON s.good_id = c.good_id
                ORDER BY c.sort_order, s.good_id
                """)
                .query((resultSet, rowNum) -> mapAdminItem(
                        resultSet.getInt("good_id"),
                        resultSet.getInt("item_id"),
                        resultSet.getString("item_name"),
                        resultSet.getInt("price_cash"),
                        resultSet.getLong("count"),
                        resultSet.getString("image_path"),
                        resultSet.getBoolean("active"),
                        resultSet.getInt("sort_order"),
                        resultSet.getBoolean("custom_cover")))
                .list();
    }

    public Optional<ShopOffer> findShopOffer(int goodId) {
        return jdbcClient.sql("""
                SELECT good_id, item_id, item_name, price_cash, count
                FROM public.shop
                WHERE good_id = :goodId
                """)
                .param("goodId", goodId)
                .query((resultSet, rowNum) -> new ShopOffer(
                        resultSet.getInt("good_id"),
                        resultSet.getInt("item_id"),
                        normalizedName(resultSet.getString("item_name")),
                        resultSet.getInt("price_cash"),
                        resultSet.getLong("count")))
                .optional();
    }

    public boolean updateShopOfferPrice(int goodId, int priceCash) {
        return jdbcClient.sql("""
                UPDATE public.shop
                SET price_cash = :priceCash,
                    visibility = 4
                WHERE good_id = :goodId
                """)
                .param("goodId", goodId)
                .param("priceCash", priceCash)
                .update() > 0;
    }

    public void saveCatalogItem(int goodId, String displayName, String filename, String contentType, byte[] coverData) {
        String coverPath = "/api/v1/webstore/items/" + goodId + "/cover";
        jdbcClient.sql("""
                INSERT INTO public.webstore_catalog
                    (good_id, image_path, sort_order, active, display_name,
                     cover_filename, cover_content_type, cover_data)
                VALUES
                    (:goodId, :imagePath,
                     COALESCE((SELECT MAX(sort_order) + 10 FROM public.webstore_catalog), 10),
                     TRUE, :displayName, :filename, :contentType, :coverData)
                ON CONFLICT (good_id) DO UPDATE SET
                    image_path = EXCLUDED.image_path,
                    active = TRUE,
                    display_name = EXCLUDED.display_name,
                    cover_filename = EXCLUDED.cover_filename,
                    cover_content_type = EXCLUDED.cover_content_type,
                    cover_data = EXCLUDED.cover_data
                """)
                .param("goodId", goodId)
                .param("imagePath", coverPath)
                .param("displayName", displayName)
                .param("filename", filename)
                .param("contentType", contentType)
                .param("coverData", coverData)
                .update();
    }

    public Optional<AdminWebstoreItem> findAdminCatalogItem(int goodId) {
        return findAdminCatalog().stream()
                .filter(item -> item.goodId() == goodId)
                .findFirst();
    }

    public Optional<WebstoreCover> findCover(int goodId) {
        return jdbcClient.sql("""
                SELECT c.cover_data, c.cover_content_type
                FROM public.webstore_catalog c
                JOIN public.shop s ON s.good_id = c.good_id
                WHERE c.good_id = :goodId
                  AND c.active = TRUE
                  AND s.visibility = 4
                  AND s.price_cash > 0
                  AND c.cover_data IS NOT NULL
                """)
                .param("goodId", goodId)
                .query((resultSet, rowNum) -> new WebstoreCover(
                        resultSet.getBytes("cover_data"),
                        resultSet.getString("cover_content_type")))
                .optional();
    }

    public Optional<Integer> deductCash(long playerId, int priceCash) {
        return jdbcClient.sql("""
                UPDATE public.accounts
                SET money = money - :priceCash
                WHERE player_id = :playerId
                  AND banned = 0
                  AND money >= :priceCash
                RETURNING money
                """)
                .param("playerId", playerId)
                .param("priceCash", priceCash)
                .query(Integer.class)
                .optional();
    }

    public boolean reservePurchaseEntitlement(long playerId, int itemId) {
        return jdbcClient.sql("""
                INSERT INTO public.webstore_purchase_entitlements (player_id, item_id)
                VALUES (:playerId, :itemId)
                ON CONFLICT (player_id, item_id) DO NOTHING
                RETURNING item_id
                """)
                .param("playerId", playerId)
                .param("itemId", itemId)
                .query(Integer.class)
                .optional()
                .isPresent();
    }

    public List<Integer> findPurchasedItemIds(long playerId) {
        return jdbcClient.sql("""
                SELECT item_id
                FROM public.webstore_purchase_entitlements
                WHERE player_id = :playerId
                ORDER BY item_id
                """)
                .param("playerId", playerId)
                .query(Integer.class)
                .list();
    }

    public void linkPurchaseEntitlement(long playerId, int itemId, long purchaseId) {
        jdbcClient.sql("""
                UPDATE public.webstore_purchase_entitlements
                SET purchase_id = :purchaseId
                WHERE player_id = :playerId
                  AND item_id = :itemId
                  AND purchase_id IS NULL
                """)
                .param("playerId", playerId)
                .param("itemId", itemId)
                .param("purchaseId", purchaseId)
                .update();
    }

    public long deliverItem(long playerId, PurchaseItem item) {
        return jdbcClient.sql("""
                INSERT INTO public.player_items
                    (owner_id, item_id, item_name, count, category, equip)
                VALUES
                    (:playerId, :itemId, :itemName, :itemCount, :category, 1)
                RETURNING object_id
                """)
                .param("playerId", playerId)
                .param("itemId", item.itemId())
                .param("itemName", item.itemName())
                .param("itemCount", item.count())
                .param("category", categoryCode(item.itemId()))
                .query(Long.class)
                .single();
    }

    public WebstorePurchaseResponse recordPurchase(
            UUID requestId,
            long playerId,
            PurchaseItem item,
            int cashAfter,
            long deliveredObjectId) {
        return jdbcClient.sql("""
                INSERT INTO public.webstore_purchases
                    (request_id, player_id, good_id, item_id, item_name,
                     price_cash, item_count, cash_after, delivered_object_id)
                VALUES
                    (:requestId, :playerId, :goodId, :itemId, :itemName,
                     :priceCash, :itemCount, :cashAfter, :deliveredObjectId)
                RETURNING purchase_id, purchased_at
                """)
                .param("requestId", requestId)
                .param("playerId", playerId)
                .param("goodId", item.goodId())
                .param("itemId", item.itemId())
                .param("itemName", item.itemName())
                .param("priceCash", item.priceCash())
                .param("itemCount", item.count())
                .param("cashAfter", cashAfter)
                .param("deliveredObjectId", deliveredObjectId)
                .query((resultSet, rowNum) -> new WebstorePurchaseResponse(
                        resultSet.getLong("purchase_id"),
                        item.goodId(),
                        item.itemId(),
                        item.itemName(),
                        item.priceCash(),
                        item.count(),
                        countLabel(item.count()),
                        cashAfter,
                        resultSet.getObject("purchased_at", OffsetDateTime.class),
                        false))
                .single();
    }

    public Optional<WebstorePurchaseResponse> findPurchase(long playerId, UUID requestId) {
        return jdbcClient.sql("""
                SELECT purchase_id, good_id, item_id, item_name, price_cash,
                       item_count, cash_after, purchased_at
                FROM public.webstore_purchases
                WHERE player_id = :playerId
                  AND request_id = :requestId
                """)
                .param("playerId", playerId)
                .param("requestId", requestId)
                .query((resultSet, rowNum) -> new WebstorePurchaseResponse(
                        resultSet.getLong("purchase_id"),
                        resultSet.getInt("good_id"),
                        resultSet.getInt("item_id"),
                        resultSet.getString("item_name"),
                        resultSet.getInt("price_cash"),
                        resultSet.getLong("item_count"),
                        countLabel(resultSet.getLong("item_count")),
                        resultSet.getInt("cash_after"),
                        resultSet.getObject("purchased_at", OffsetDateTime.class),
                        true))
                .optional();
    }

    public List<WebstoreHistoryItem> findPurchaseHistory(long playerId, OffsetDateTime fromDate) {
        return jdbcClient.sql("""
                SELECT p.purchase_id, p.good_id, p.item_id, p.item_name, p.price_cash,
                       p.item_count, p.cash_after, p.purchased_at, p.recovery_count,
                       CASE WHEN c.cover_data IS NOT NULL
                            THEN '/api/v1/webstore/items/' || p.good_id || '/cover'
                            ELSE c.image_path
                       END AS image_path,
                       EXISTS (
                           SELECT 1
                           FROM public.player_items pi
                           WHERE pi.object_id = p.delivered_object_id
                             AND pi.owner_id = p.player_id
                             AND pi.item_id = p.item_id
                       ) AS delivered
                FROM public.webstore_purchases p
                LEFT JOIN public.webstore_catalog c ON c.good_id = p.good_id
                WHERE p.player_id = :playerId
                  AND p.purchased_at >= :fromDate
                ORDER BY p.purchased_at DESC, p.purchase_id DESC
                LIMIT 100
                """)
                .param("playerId", playerId)
                .param("fromDate", fromDate)
                .query((resultSet, rowNum) -> new WebstoreHistoryItem(
                        resultSet.getLong("purchase_id"),
                        resultSet.getInt("good_id"),
                        resultSet.getInt("item_id"),
                        resultSet.getString("item_name"),
                        resultSet.getInt("price_cash"),
                        resultSet.getLong("item_count"),
                        countLabel(resultSet.getLong("item_count")),
                        resultSet.getInt("cash_after"),
                        resultSet.getObject("purchased_at", OffsetDateTime.class),
                        resultSet.getString("image_path"),
                        resultSet.getBoolean("delivered"),
                        !resultSet.getBoolean("delivered") && resultSet.getInt("recovery_count") == 0,
                        resultSet.getInt("recovery_count")))
                .list();
    }

    public Optional<PurchaseDelivery> findPurchaseDeliveryForUpdate(long playerId, long purchaseId) {
        return jdbcClient.sql("""
                SELECT purchase_id, good_id, item_id, item_name, price_cash, item_count,
                       delivered_object_id, recovery_count
                FROM public.webstore_purchases
                WHERE purchase_id = :purchaseId
                  AND player_id = :playerId
                FOR UPDATE
                """)
                .param("purchaseId", purchaseId)
                .param("playerId", playerId)
                .query((resultSet, rowNum) -> new PurchaseDelivery(
                        resultSet.getLong("purchase_id"),
                        resultSet.getInt("good_id"),
                        resultSet.getInt("item_id"),
                        resultSet.getString("item_name"),
                        resultSet.getInt("price_cash"),
                        resultSet.getLong("item_count"),
                        resultSet.getObject("delivered_object_id", Long.class),
                        resultSet.getInt("recovery_count")))
                .optional();
    }

    public boolean deliveryExists(long playerId, long objectId, int itemId) {
        return jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.player_items
                    WHERE object_id = :objectId
                      AND owner_id = :playerId
                      AND item_id = :itemId
                )
                """)
                .param("objectId", objectId)
                .param("playerId", playerId)
                .param("itemId", itemId)
                .query(Boolean.class)
                .single();
    }

    public Optional<Long> findUnlinkedDeliveryObject(long playerId, int itemId) {
        return jdbcClient.sql("""
                SELECT pi.object_id
                FROM public.player_items pi
                WHERE pi.owner_id = :playerId
                  AND pi.item_id = :itemId
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.webstore_purchases p
                      WHERE p.delivered_object_id = pi.object_id
                  )
                ORDER BY pi.object_id DESC
                LIMIT 1
                FOR UPDATE OF pi
                """)
                .param("playerId", playerId)
                .param("itemId", itemId)
                .query(Long.class)
                .optional();
    }

    public void linkDelivery(long purchaseId, long objectId) {
        jdbcClient.sql("""
                UPDATE public.webstore_purchases
                SET delivered_object_id = :objectId
                WHERE purchase_id = :purchaseId
                  AND delivered_object_id IS NULL
                """)
                .param("purchaseId", purchaseId)
                .param("objectId", objectId)
                .update();
    }

    public void recordRecovery(long purchaseId, long objectId) {
        jdbcClient.sql("""
                UPDATE public.webstore_purchases
                SET delivered_object_id = :objectId,
                    recovery_count = recovery_count + 1,
                    recovered_at = CURRENT_TIMESTAMP
                WHERE purchase_id = :purchaseId
                  AND recovery_count = 0
                """)
                .param("purchaseId", purchaseId)
                .param("objectId", objectId)
                .update();
    }

    private static WebstoreItem mapItem(
            int goodId,
            int itemId,
            String itemName,
            int priceCash,
            long count,
            int tag,
            String imagePath) {
        String category = category(itemId);
        return new WebstoreItem(
                goodId,
                itemId,
                normalizedName(itemName),
                priceCash,
                count,
                countLabel(count),
                category,
                categoryLabel(category),
                tag,
                tagLabel(tag),
                imagePath);
    }

    private static AdminWebstoreItem mapAdminItem(
            int goodId,
            int itemId,
            String itemName,
            int priceCash,
            long count,
            String imagePath,
            boolean active,
            int sortOrder,
            boolean customCover) {
        String category = category(itemId);
        return new AdminWebstoreItem(
                goodId,
                itemId,
                normalizedName(itemName),
                priceCash,
                count,
                countLabel(count),
                category,
                categoryLabel(category),
                imagePath,
                active,
                sortOrder,
                customCover);
    }

    static String countLabel(long count) {
        if (count < 86_400) {
            return count == 1 ? "1 Unidade" : count + " Unidades";
        }
        long days = Math.max(1, count / 86_400);
        return days == 1 ? "1 Dia" : days + " Dias";
    }

    private static String normalizedName(String value) {
        return value == null ? "" : value.replace('_', ' ').trim();
    }

    private static int categoryCode(int itemId) {
        return switch (category(itemId)) {
            case "CHARACTER" -> 2;
            case "ITEM" -> 3;
            default -> 1;
        };
    }

    private static String category(int itemId) {
        if (itemId >= 600_000 && itemId < 700_000) return "CHARACTER";
        if (itemId > 0 && itemId < 600_000) return "WEAPON";
        return "ITEM";
    }

    private static String categoryLabel(String category) {
        return switch (category) {
            case "WEAPON" -> "Arma";
            case "CHARACTER" -> "Personagem";
            default -> "Item";
        };
    }

    private static String tagLabel(int tag) {
        return switch (tag) {
            case 1 -> "Novo";
            case 2 -> "Promoção";
            case 3 -> "Recomendado";
            case 4 -> "Evento";
            default -> "";
        };
    }

    public record PurchaseItem(int goodId, int itemId, String itemName, int priceCash, long count) {
    }

    public record PurchaseDelivery(
            long purchaseId,
            int goodId,
            int itemId,
            String itemName,
            int priceCash,
            long itemCount,
            Long deliveredObjectId,
            int recoveryCount) {
    }

    public record ShopOffer(int goodId, int itemId, String itemName, int priceCash, long count) {
    }
}
