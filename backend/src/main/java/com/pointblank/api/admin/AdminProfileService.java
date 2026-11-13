package com.pointblank.api.admin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.pointblank.api.account.AccountRepository;

@Service
public class AdminProfileService {
    private static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private final AccountRepository accounts;
    private final AdminProfileRepository profiles;

    public AdminProfileService(AccountRepository accounts, AdminProfileRepository profiles) {
        this.accounts = accounts;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public AdminProfile find(long playerId) {
        requireAdministrator(playerId);
        return profiles.find(playerId);
    }

    @Transactional(readOnly = true)
    public AdminProfileImage findImage(long playerId) {
        requireAdministrator(playerId);
        return profiles.findImage(playerId)
                .orElseThrow(() -> new AdminAccountException(HttpStatus.NOT_FOUND, "Foto de perfil não encontrada"));
    }

    @Transactional
    public AdminProfile update(long playerId, UpdateAdminProfileRequest request, MultipartFile photo) {
        requireAdministrator(playerId);
        String name = request.displayName();
        if (name == null || name.length() > 32 || name.codePoints().anyMatch(Character::isISOControl)) {
            throw invalid("O apelido deve ter até 32 caracteres e não pode conter caracteres de controle");
        }
        if (photo != null && request.removeAvatar()) throw invalid("Escolha entre substituir ou remover a foto");
        AdminProfileImage image = photo == null ? null : validateImage(photo);
        profiles.saveName(playerId, name.strip());
        if (image != null) profiles.saveImage(playerId, image);
        else if (request.removeAvatar()) profiles.removeImage(playerId);
        return profiles.find(playerId);
    }

    private void requireAdministrator(long playerId) {
        var account = accounts.findByPlayerId(playerId)
                .orElseThrow(() -> new AdminAccountException(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
        if (account.accessLevel() < 5 || account.banned()) {
            throw new AdminAccountException(HttpStatus.FORBIDDEN, "Acesso administrativo necessário");
        }
    }

    private AdminProfileImage validateImage(MultipartFile photo) {
        if (photo.isEmpty()) throw invalid("Selecione uma foto JPG ou PNG");
        if (photo.getSize() > MAX_IMAGE_BYTES) throw invalid("A foto deve possuir no máximo 10 MB");
        try {
            byte[] original = photo.getBytes();
            try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(original))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalid("O arquivo não é uma imagem JPG ou PNG válida");
                ImageReader reader = readers.next();
                try {
                    String type = switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                        case "jpeg", "jpg" -> "image/jpeg";
                        case "png" -> "image/png";
                        default -> throw invalid("Use uma foto JPG ou PNG");
                    };
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || width > 10000 || height > 10000 || (long) width * height > 40000000) {
                        throw invalid("A foto deve ter até 40 megapixels e 10.000 pixels por lado");
                    }
                    // Decode a bounded sample to validate the file. The stored bytes stay untouched.
                    var params = reader.getDefaultReadParam();
                    int sample = Math.max(1, (Math.max(width, height) + 1023) / 1024);
                    params.setSourceSubsampling(sample, sample, 0, 0);
                    reader.read(0, params).flush();
                    String version = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original));
                    return new AdminProfileImage(original, type, width, height, version);
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException exception) {
            throw invalid("Não foi possível ler a foto. Envie uma imagem JPG ou PNG válida");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static AdminAccountException invalid(String message) {
        return new AdminAccountException(HttpStatus.BAD_REQUEST, message);
    }
}
