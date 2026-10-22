package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.web.RegraNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/** Salva as fotos do portfólio na pasta de uploads. */
@Service
public class ArmazenamentoService {

    private static final Map<String, String> TIPOS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path pasta;

    public ArmazenamentoService(@Value("${atelie.uploads-dir}") String pasta) throws IOException {
        this.pasta = Path.of(pasta).toAbsolutePath().normalize();
        Files.createDirectories(this.pasta);
    }

    public Path getPasta() {
        return pasta;
    }

    public String salvarImagem(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Envie uma imagem.");
        }
        String extensao = TIPOS.get(arquivo.getContentType());
        if (extensao == null) {
            throw new RegraNegocioException("Formato não aceito. Use JPG, PNG ou WEBP.");
        }
        try (InputStream in = arquivo.getInputStream()) {
            byte[] cabecalho = in.readNBytes(12);
            if (!assinaturaValida(cabecalho, extensao)) {
                throw new RegraNegocioException("O arquivo enviado não é uma imagem válida.");
            }
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível ler a imagem.");
        }
        String nome = UUID.randomUUID() + extensao;
        try (InputStream in = arquivo.getInputStream()) {
            Files.copy(in, pasta.resolve(nome), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível salvar a imagem.");
        }
        return nome;
    }

    public void excluir(String nome) {
        if (nome == null || nome.contains("/") || nome.contains("\\") || nome.contains("..")) {
            return;
        }
        try {
            Files.deleteIfExists(pasta.resolve(nome));
        } catch (IOException ignorado) {
            // a foto some do site mesmo que o arquivo não seja apagado
        }
    }

    private boolean assinaturaValida(byte[] b, String ext) {
        if (b.length < 12) {
            return false;
        }
        return switch (ext) {
            case ".jpg" -> (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
            case ".png" -> (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case ".webp" -> b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
    }
}
