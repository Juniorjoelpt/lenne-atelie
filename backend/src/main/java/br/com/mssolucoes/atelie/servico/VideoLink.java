package br.com.mssolucoes.atelie.servico;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converte o link que a Lenne cola (YouTube não listado, Vimeo ou Google Drive) no endereço do player
 * que vai dentro da página. Qualquer outro endereço é recusado, para o site só embutir players conhecidos.
 */
public final class VideoLink {

    private static final Pattern ID_YOUTUBE = Pattern.compile("[A-Za-z0-9_-]{6,20}");
    private static final Pattern VIMEO = Pattern.compile("^/(?:video/)?(\\d{5,12})(?:/([0-9a-f]{6,20}))?/?$");
    private static final Pattern DRIVE = Pattern.compile("^/file/d/([A-Za-z0-9_-]{10,80})(?:/.*)?$");

    private VideoLink() {
    }

    /** Endereço do player, ou null se o link não for de um serviço aceito. */
    public static String embed(String link) {
        if (link == null || link.isBlank()) {
            return null;
        }
        URI u;
        try {
            u = URI.create(link.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (u.getHost() == null || !"https".equalsIgnoreCase(u.getScheme()) && !"http".equalsIgnoreCase(u.getScheme())) {
            return null;
        }
        String host = u.getHost().toLowerCase().replaceFirst("^(www\\.|m\\.)", "");
        String caminho = u.getPath() == null ? "" : u.getPath();
        String query = u.getRawQuery() == null ? "" : u.getRawQuery();

        String idYoutube = switch (host) {
            case "youtu.be" -> caminho.replaceFirst("^/", "").split("/")[0];
            case "youtube.com", "youtube-nocookie.com" -> {
                if (caminho.equals("/watch")) {
                    Matcher m = Pattern.compile("(?:^|&)v=([^&]+)").matcher(query);
                    yield m.find() ? m.group(1) : null;
                }
                Matcher m = Pattern.compile("^/(?:embed|shorts|live)/([^/?]+)").matcher(caminho);
                yield m.find() ? m.group(1) : null;
            }
            default -> null;
        };
        if (idYoutube != null) {
            return ID_YOUTUBE.matcher(idYoutube).matches()
                    ? "https://www.youtube-nocookie.com/embed/" + idYoutube + "?rel=0&modestbranding=1" : null;
        }
        if (host.equals("vimeo.com") || host.equals("player.vimeo.com")) {
            Matcher m = VIMEO.matcher(caminho);
            if (!m.matches()) {
                return null;
            }
            String hash = m.group(2);
            if (hash == null) {
                Matcher h = Pattern.compile("(?:^|&)h=([0-9a-f]{6,20})").matcher(query);
                hash = h.find() ? h.group(1) : null;
            }
            return "https://player.vimeo.com/video/" + m.group(1) + (hash != null ? "?h=" + hash : "");
        }
        if (host.equals("drive.google.com")) {
            Matcher m = DRIVE.matcher(caminho);
            return m.matches() ? "https://drive.google.com/file/d/" + m.group(1) + "/preview" : null;
        }
        return null;
    }
}
