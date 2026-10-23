package br.com.mssolucoes.atelie.servico;

import br.com.mssolucoes.atelie.dominio.Configuracao;
import br.com.mssolucoes.atelie.dominio.Enums.StatusInscricao;
import br.com.mssolucoes.atelie.dominio.Enums.StatusOficina;
import br.com.mssolucoes.atelie.dominio.Inscricao;
import br.com.mssolucoes.atelie.dominio.Oficina;
import br.com.mssolucoes.atelie.dto.Dtos.CertificadoPublico;
import br.com.mssolucoes.atelie.repositorio.InscricaoRepository;
import br.com.mssolucoes.atelie.repositorio.OficinaRepository;
import br.com.mssolucoes.atelie.servico.DocumentoService.Arquivo;
import br.com.mssolucoes.atelie.web.NaoEncontradoException;
import br.com.mssolucoes.atelie.web.RegraNegocioException;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static br.com.mssolucoes.atelie.servico.pdf.PdfAtelie.*;

/**
 * Certificado de participação da oficina, em A4 deitado, com as fontes do site (Allura e Nunito).
 * A aluna também baixa o próprio certificado pelo link com o código da inscrição.
 */
@Service
public class CertificadoService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter EXTENSO = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PT_BR);
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("d 'de' MMMM", PT_BR);
    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F-]{36}");
    private static final int LIMITE_CONTEUDO = 380;

    private final InscricaoRepository inscricoes;
    private final OficinaRepository oficinas;
    private final ConfiguracaoService configuracao;
    private final UrlPublica url;
    private final BaseFont cursiva;
    private final BaseFont regular;
    private final BaseFont negrito;

    public CertificadoService(InscricaoRepository inscricoes, OficinaRepository oficinas,
                              ConfiguracaoService configuracao, UrlPublica url) throws IOException {
        this.inscricoes = inscricoes;
        this.oficinas = oficinas;
        this.configuracao = configuracao;
        this.url = url;
        this.cursiva = fonte("Allura-Regular.ttf");
        this.regular = fonte("Nunito-Regular.ttf");
        this.negrito = fonte("Nunito-ExtraBold.ttf");
    }

    @Transactional(readOnly = true)
    public Arquivo daInscricao(Long inscricaoId) {
        Inscricao i = inscricoes.findById(inscricaoId).orElseThrow(() -> new NaoEncontradoException("Inscrição"));
        if (i.getStatus() != StatusInscricao.CONFIRMADA) {
            throw new RegraNegocioException("Só alunas com inscrição confirmada recebem certificado.");
        }
        return new Arquivo("Certificado-" + DocumentoService.nomeArquivo(i.getNome()) + ".pdf", gerar(List.of(i)));
    }

    @Transactional(readOnly = true)
    public Arquivo daTurma(Long oficinaId) {
        Oficina o = oficinas.findById(oficinaId).orElseThrow(() -> new NaoEncontradoException("Oficina"));
        List<Inscricao> confirmadas = inscricoes.findByOficinaIdOrderByCriadoEmAsc(oficinaId).stream()
                .filter(i -> i.getStatus() == StatusInscricao.CONFIRMADA)
                .sorted(java.util.Comparator.comparing(i -> i.getNome().toLowerCase(PT_BR)))
                .toList();
        if (confirmadas.isEmpty()) {
            throw new RegraNegocioException("Esta oficina ainda não tem alunas confirmadas.");
        }
        return new Arquivo("Certificados-" + DocumentoService.nomeArquivo(o.getTitulo()) + ".pdf", gerar(confirmadas));
    }

    /** Página pública: confirma que o certificado existe (e serve de verificação). */
    @Transactional(readOnly = true)
    public CertificadoPublico verificar(String codigo) {
        Inscricao i = publica(codigo);
        Oficina o = i.getOficina();
        return new CertificadoPublico(i.getNome(), o.getTitulo(), o.getInicio().toLocalDate(),
                OficinaService.cargaHorariaCertificado(o), o.getLocal(), configuracao.atual().getNomeAtelie(),
                disponivel(i));
    }

    @Transactional(readOnly = true)
    public Arquivo publico(String codigo) {
        Inscricao i = publica(codigo);
        if (!disponivel(i)) {
            throw new RegraNegocioException("O certificado fica disponível depois que a oficina começar.");
        }
        return new Arquivo("Certificado-" + DocumentoService.nomeArquivo(i.getNome()) + ".pdf", gerar(List.of(i)));
    }

    private Inscricao publica(String codigo) {
        if (codigo == null || !UUID.matcher(codigo).matches()) {
            throw new NaoEncontradoException("Certificado");
        }
        Inscricao i = inscricoes.findByCodigo(codigo).orElseThrow(() -> new NaoEncontradoException("Certificado"));
        if (i.getStatus() != StatusInscricao.CONFIRMADA || i.getOficina().getStatus() == StatusOficina.CANCELADA) {
            throw new NaoEncontradoException("Certificado");
        }
        return i;
    }

    private static boolean disponivel(Inscricao i) {
        return !i.getOficina().getInicio().isAfter(LocalDateTime.now());
    }

    // ---------- PDF ----------

    private byte[] gerar(List<Inscricao> lista) {
        Configuracao cfg = configuracao.atual();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rectangle pagina = PageSize.A4.rotate();
        Document doc = new Document(pagina, 70, 70, 58, 150);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new Moldura());
        doc.addTitle(lista.size() == 1 ? "Certificado — " + lista.get(0).getNome() : "Certificados — " + lista.get(0).getOficina().getTitulo());
        doc.addAuthor(cfg.getNomeAtelie());
        doc.addCreator("Sistema do ateliê — MS Soluções Inteligentes");
        doc.open();
        for (int n = 0; n < lista.size(); n++) {
            if (n > 0) {
                doc.newPage();
            }
            pagina(doc, writer, cfg, lista.get(n));
        }
        doc.close();
        return out.toByteArray();
    }

    private void pagina(Document doc, PdfWriter writer, Configuracao cfg, Inscricao i) {
        Oficina o = i.getOficina();

        Paragraph atelie = centro(espacado(cfg.getNomeAtelie().toUpperCase(PT_BR), new Font(negrito, 10, Font.NORMAL, TERRA), 2.2f));
        atelie.setSpacingBefore(18);
        adicionar(doc, atelie);

        Paragraph titulo = centro(new Phrase("Certificado", new Font(cursiva, 66, Font.NORMAL, MARROM)));
        titulo.setLeading(64);
        titulo.setSpacingBefore(4);
        adicionar(doc, titulo);
        Paragraph sub = centro(espacado("DE PARTICIPAÇÃO", new Font(negrito, 11, Font.NORMAL, TERRA), 3f));
        sub.setSpacingBefore(16);
        adicionar(doc, sub);

        Paragraph certificamos = centro(new Phrase("Certificamos que", new Font(regular, 13, Font.NORMAL, MARROM_2)));
        certificamos.setSpacingBefore(34);
        adicionar(doc, certificamos);

        Font fonteNome = new Font(cursiva, i.getNome().length() > 34 ? 38 : 46, Font.NORMAL, TERRA);
        Paragraph nome = centro(new Phrase(i.getNome(), fonteNome));
        nome.setLeading(54);
        nome.setSpacingBefore(4);
        adicionar(doc, nome);

        Font texto = new Font(regular, 13, Font.NORMAL, MARROM);
        Font textoForte = new Font(negrito, 13, Font.NORMAL, MARROM);
        Paragraph corpo = new Paragraph();
        corpo.setAlignment(Element.ALIGN_CENTER);
        corpo.setLeading(21);
        corpo.setSpacingBefore(14);
        corpo.add(new Chunk("participou da oficina ", texto));
        corpo.add(new Chunk(o.getTitulo(), textoForte));
        corpo.add(new Chunk(", realizada " + quando(o)
                + (o.getLocal() != null ? ", em " + o.getLocal() : "")
                + carga(OficinaService.cargaHorariaCertificado(o)) + ".", texto));
        adicionar(doc, corpo);

        if (o.getConteudoProgramatico() != null) {
            String conteudo = o.getConteudoProgramatico().replaceAll("\\s*\\n\\s*", " · ").trim();
            if (conteudo.length() > LIMITE_CONTEUDO) {
                conteudo = conteudo.substring(0, LIMITE_CONTEUDO - 1).replaceAll("\\s+\\S*$", "") + "…";
            }
            Paragraph p = new Paragraph();
            p.setAlignment(Element.ALIGN_CENTER);
            p.setLeading(15);
            p.setSpacingBefore(16);
            p.setIndentationLeft(50);
            p.setIndentationRight(50);
            p.add(new Chunk("Conteúdo: ", new Font(negrito, 10, Font.NORMAL, MARROM_2)));
            p.add(new Chunk(conteudo, new Font(regular, 10, Font.NORMAL, MARROM_2)));
            adicionar(doc, p);
        }

        rodape(writer, doc, cfg, i);
    }

    /** Assinatura no centro, cidade e data à esquerda, código de verificação à direita, sempre na mesma altura. */
    private void rodape(PdfWriter writer, Document doc, Configuracao cfg, Inscricao i) {
        float largura = doc.right() - doc.left();
        PdfPTable t = new PdfPTable(new float[]{0.95f, 1.1f, 1.35f});
        t.setTotalWidth(largura);
        t.setLockedWidth(true);

        Font pequeno = new Font(regular, 9, Font.NORMAL, MARROM_2);
        Font pequenoForte = new Font(negrito, 9, Font.NORMAL, MARROM);

        PdfPCell local = semBorda();
        local.setVerticalAlignment(Element.ALIGN_BOTTOM);
        String cidade = cfg.getCidade() != null ? cfg.getCidade() + ", " : "";
        LocalDate emissao = i.getOficina().getInicio().toLocalDate();
        local.addElement(new Paragraph(cidade + EXTENSO.format(emissao) + ".", pequeno));
        t.addCell(local);

        PdfPCell assinatura = semBorda();
        assinatura.setVerticalAlignment(Element.ALIGN_BOTTOM);
        String quem = cfg.getResponsavel() != null ? cfg.getResponsavel() : cfg.getNomeAtelie();
        Paragraph nomeAssinatura = centro(new Phrase(quem, new Font(cursiva, 26, Font.NORMAL, MARROM)));
        nomeAssinatura.setLeading(24);
        nomeAssinatura.setSpacingAfter(8);
        assinatura.addElement(nomeAssinatura);
        PdfPTable linha = new PdfPTable(1);
        linha.setWidthPercentage(86);
        PdfPCell l = new PdfPCell();
        l.setBorder(Rectangle.TOP);
        l.setBorderColor(MARROM_2);
        l.setBorderWidth(0.8f);
        l.setPaddingTop(4);
        l.addElement(centro(new Phrase(quem, pequenoForte)));
        if (cfg.getResponsavel() != null) {
            l.addElement(centro(new Phrase(cfg.getNomeAtelie(), pequeno)));
        }
        linha.addCell(l);
        assinatura.addElement(linha);
        t.addCell(assinatura);

        PdfPCell codigo = semBorda();
        codigo.setVerticalAlignment(Element.ALIGN_BOTTOM);
        Paragraph c1 = new Paragraph("Código de verificação: " + codigoCurto(i), pequenoForte);
        c1.setAlignment(Element.ALIGN_RIGHT);
        codigo.addElement(c1);
        String link = url.certificado(i.getCodigo());
        if (!link.startsWith("/")) {
            Paragraph c2 = new Paragraph(link.replaceFirst("^https?://", ""), new Font(regular, 6.5f, Font.NORMAL, MARROM_2));
            c2.setAlignment(Element.ALIGN_RIGHT);
            codigo.addElement(c2);
        }
        t.addCell(codigo);

        t.writeSelectedRows(0, -1, doc.left(), 132, writer.getDirectContent());
    }

    static String codigoCurto(Inscricao i) {
        return i.getCodigo().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String quando(Oficina o) {
        LocalDate ini = o.getInicio().toLocalDate();
        LocalDate fim = o.getFim() == null ? ini : o.getFim().toLocalDate();
        if (!fim.isAfter(ini)) {
            return "em " + EXTENSO.format(ini);
        }
        if (fim.getYear() == ini.getYear() && fim.getMonth() == ini.getMonth()) {
            return "de " + ini.getDayOfMonth() + " a " + EXTENSO.format(fim);
        }
        return "de " + (fim.getYear() == ini.getYear() ? DIA_MES.format(ini) : EXTENSO.format(ini)) + " a " + EXTENSO.format(fim);
    }

    private static String carga(BigDecimal horas) {
        if (horas == null) {
            return "";
        }
        NumberFormat f = NumberFormat.getNumberInstance(PT_BR);
        f.setMaximumFractionDigits(1);
        return ", com carga horária de " + f.format(horas) + (horas.compareTo(BigDecimal.ONE) == 0 ? " hora" : " horas");
    }

    private static Paragraph centro(Phrase f) {
        Paragraph p = new Paragraph(f);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private static Phrase espacado(String texto, Font fonte, float espaco) {
        Chunk c = new Chunk(texto, fonte);
        c.setCharacterSpacing(espaco);
        return new Phrase(c);
    }

    private static BaseFont fonte(String arquivo) throws IOException {
        try (InputStream in = new ClassPathResource("fontes/" + arquivo).getInputStream()) {
            return BaseFont.createFont(arquivo, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        }
    }

    /** Fundo cru com moldura dupla terracota/areia e nós de macramê nos cantos. */
    private static final class Moldura extends PdfPageEventHelper {
        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContentUnder();
            Rectangle p = document.getPageSize();
            cb.saveState();
            cb.setColorFill(CRU);
            cb.rectangle(0, 0, p.getWidth(), p.getHeight());
            cb.fill();

            cb.setColorStroke(TERRA);
            cb.setLineWidth(2.2f);
            cb.roundRectangle(22, 22, p.getWidth() - 44, p.getHeight() - 44, 14);
            cb.stroke();
            cb.setColorStroke(AREIA);
            cb.setLineWidth(0.9f);
            cb.roundRectangle(32, 32, p.getWidth() - 64, p.getHeight() - 64, 10);
            cb.stroke();

            float[][] cantos = {{32, 32}, {p.getWidth() - 32, 32}, {32, p.getHeight() - 32}, {p.getWidth() - 32, p.getHeight() - 32}};
            for (float[] c : cantos) {
                no(cb, c[0], c[1]);
            }
            cb.restoreState();
        }

        /** Três argolas entrelaçadas, como um nó de macramê. */
        private static void no(PdfContentByte cb, float x, float y) {
            cb.setColorFill(CRU);
            cb.circle(x, y, 10);
            cb.fill();
            Color[] cores = {TERRA, OLIVA, TERRA};
            float[][] pos = {{-5, 0}, {0, 0}, {5, 0}};
            for (int k = 0; k < pos.length; k++) {
                cb.setColorStroke(cores[k]);
                cb.setLineWidth(1.4f);
                cb.circle(x + pos[k][0], y + pos[k][1], 4.2f);
                cb.stroke();
            }
        }
    }
}
