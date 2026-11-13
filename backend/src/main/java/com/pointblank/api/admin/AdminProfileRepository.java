package com.pointblank.api.admin;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AdminProfileRepository {
    private final JdbcClient jdbc;

    public AdminProfileRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public AdminProfile find(long playerId) {
        return jdbc.sql("""
                SELECT display_name, avatar_version, avatar_width, avatar_height,
                       octet_length(avatar_data)::bigint AS avatar_size
                FROM public.web_admin_profiles WHERE player_id = :playerId
                """).param("playerId", playerId)
                .query((rs, row) -> new AdminProfile(rs.getString("display_name"), rs.getString("avatar_version"),
                        rs.getObject("avatar_width", Integer.class), rs.getObject("avatar_height", Integer.class),
                        rs.getObject("avatar_size", Long.class)))
                .optional().orElseGet(AdminProfile::empty);
    }

    public Optional<AdminProfileImage> findImage(long playerId) {
        return jdbc.sql("""
                SELECT avatar_data, avatar_content_type, avatar_width, avatar_height, avatar_version
                FROM public.web_admin_profiles WHERE player_id = :playerId AND avatar_data IS NOT NULL
                """).param("playerId", playerId)
                .query((rs, row) -> new AdminProfileImage(rs.getBytes("avatar_data"), rs.getString("avatar_content_type"),
                        rs.getInt("avatar_width"), rs.getInt("avatar_height"), rs.getString("avatar_version")))
                .optional();
    }

    public void saveName(long playerId, String displayName) {
        jdbc.sql("""
                INSERT INTO public.web_admin_profiles (player_id, display_name) VALUES (:playerId, :name)
                ON CONFLICT (player_id) DO UPDATE SET display_name = EXCLUDED.display_name, updated_at = CURRENT_TIMESTAMP
                """).param("playerId", playerId).param("name", displayName).update();
    }

    public void saveImage(long playerId, AdminProfileImage image) {
        jdbc.sql("""
                UPDATE public.web_admin_profiles
                SET avatar_data = :data, avatar_content_type = :type, avatar_width = :width,
                    avatar_height = :height, avatar_version = :version, updated_at = CURRENT_TIMESTAMP
                WHERE player_id = :playerId
                """).param("playerId", playerId).param("data", image.data()).param("type", image.contentType())
                .param("width", image.width()).param("height", image.height()).param("version", image.version()).update();
    }

    public void removeImage(long playerId) {
        jdbc.sql("""
                UPDATE public.web_admin_profiles
                SET avatar_data = NULL, avatar_content_type = NULL, avatar_width = NULL,
                    avatar_height = NULL, avatar_version = NULL, updated_at = CURRENT_TIMESTAMP
                WHERE player_id = :playerId
                """).param("playerId", playerId).update();
    }
}
