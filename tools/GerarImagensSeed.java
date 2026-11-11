import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Gera as ilustracoes ORIGINAIS (formas vetoriais simples desenhadas com Java2D) dos produtos de
 * demonstracao. Nada e baixado nem copiado de terceiros. A saida e deterministica.
 *
 * Uso (JDK 21, a partir da raiz do repositorio):
 *   java tools/GerarImagensSeed.java src/main/resources/seed/img
 */
public class GerarImagensSeed {

    static final int T = 256;

    // slug, tipo, categoria (cor de fundo), cor principal, cor secundaria, cor terciaria
    static final String[][] PRODUTOS = {
        {"coxinha", "teardrop", "salgado", "D9892B", "F2C26B", "B36A1B"},
        {"pastel-queijo", "halfmoon", "salgado", "E0A040", "F5D58A", "F4D03F"},
        {"pastel-carne", "halfmoon", "salgado", "C98532", "EBC27A", "8A4B2A"},
        {"pao-de-queijo", "round", "salgado", "E8B45C", "F7DC9E", "C98A3A"},
        {"empada", "round", "salgado", "D6A15B", "F0D29B", "B5793A"},
        {"esfiha", "esfiha", "salgado", "C8883E", "B33A2A", "E9C46A"},
        {"quibe", "oval", "salgado", "8A5A2B", "B88049", "5E3A1A"},
        {"enroladinho", "roll", "salgado", "D99A4A", "B5412F", "F2C26B"},
        {"misto-quente", "sandwich", "lanche", "E3B068", "F4D03F", "E58E9A"},
        {"x-burguer", "sandwich", "lanche", "D99A4A", "6B3E26", "F4B400"},
        {"sanduiche-natural", "sandwich", "lanche", "F0DDB0", "E9784B", "7BC47F"},
        {"cachorro-quente", "hotdog", "lanche", "E3B068", "C0503A", "F4B400"},
        {"pao-na-chapa", "bread", "lanche", "E0B070", "F2D9A0", "B9833F"},
        {"hamburguer-frango", "sandwich", "lanche", "E0A857", "F0C27A", "5FA84A"},
        {"tapioca", "halfmoon", "lanche", "EFE6CF", "F8F2E2", "F4D03F"},
        {"wrap-frango", "roll", "lanche", "E7C98B", "5FA84A", "F4E3B8"},
        {"brigadeiro", "ball", "doce", "4A2C1A", "7A4A2E", "E8D5B5"},
        {"beijinho", "ball", "doce", "F3EFE2", "FFFFFF", "E8D5B5"},
        {"bolo-cenoura", "slice", "doce", "D9792B", "4A2C1A", "F2A65A"},
        {"pudim", "slice", "doce", "E8B34B", "7A3E12", "F5D58A"},
        {"cookie", "cookie", "doce", "C8924F", "4A2C1A", "E0B070"},
        {"donut", "donut", "doce", "F5A3B7", "FFFFFF", "D9892B"},
        {"suco-laranja", "cup", "bebida", "F39C1E", "FFFFFF", "E67E22"},
        {"suco-maracuja", "cup", "bebida", "F2C94C", "FFFFFF", "D4A017"},
        {"refrigerante", "can", "bebida", "C0392B", "F2F2F2", "8E2A20"},
        {"agua", "bottle", "bebida", "8FD3F4", "FFFFFF", "3A9BD0"},
        {"cafe", "mug", "bebida", "5B3A29", "FFFFFF", "F2F2F2"},
        {"achocolatado", "cup", "bebida", "6B4226", "FFFFFF", "4A2C1A"},
        {"cha-limao", "bottle", "bebida", "E8D44D", "FFFFFF", "C9B52A"},
    };

    static Color cor(String hex) {
        return new Color(Integer.parseInt(hex, 16));
    }

    static Color fundo(String categoria) {
        switch (categoria) {
            case "salgado": return cor("FFF1D6");
            case "lanche": return cor("FBE4D8");
            case "doce": return cor("FBE0EA");
            default: return cor("DFF1F7");
        }
    }

    public static void main(String[] args) throws Exception {
        File pasta = new File(args.length > 0 ? args[0] : "src/main/resources/seed/img");
        pasta.mkdirs();
        for (String[] p : PRODUTOS) {
            BufferedImage img = new BufferedImage(T, T, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color f = fundo(p[2]);
            g.setPaint(new GradientPaint(0, 0, f.brighter(), 0, T, f));
            g.fillRect(0, 0, T, T);
            // prato e sombra
            g.setColor(new Color(0, 0, 0, 30));
            g.fill(new Ellipse2D.Double(36, 186, 184, 34));
            Color c1 = cor(p[3]), c2 = cor(p[4]), c3 = cor(p[5]);
            desenhar(g, p[1], c1, c2, c3, new Random(p[0].hashCode()));
            g.dispose();
            File saida = new File(pasta, p[0] + ".png");
            ImageIO.write(img, "png", saida);
            if (saida.length() > 30 * 1024) {
                throw new IllegalStateException(saida + " passou de 30 KB");
            }
            System.out.println(saida.getName() + " " + saida.length() + " bytes");
        }
    }

    static void contorno(Graphics2D g, Shape s, Color c) {
        g.setColor(c.darker());
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    static void preencher(Graphics2D g, Shape s, Color c) {
        g.setColor(c);
        g.fill(s);
        contorno(g, s, c);
    }

    static void pontos(Graphics2D g, Shape area, Color c, int n, double raio, Random r) {
        g.setColor(c);
        int feitos = 0, tentativas = 0;
        while (feitos < n && tentativas++ < 2000) {
            double x = 30 + r.nextDouble() * 196, y = 30 + r.nextDouble() * 190;
            if (area.contains(x, y) && area.contains(x - raio, y) && area.contains(x + raio, y)) {
                g.fill(new Ellipse2D.Double(x - raio, y - raio, raio * 2, raio * 2));
                feitos++;
            }
        }
    }

    static void desenhar(Graphics2D g, String tipo, Color c1, Color c2, Color c3, Random r) {
        switch (tipo) {
            case "teardrop": {
                Path2D p = new Path2D.Double();
                p.moveTo(128, 34);
                p.curveTo(205, 115, 222, 205, 128, 214);
                p.curveTo(34, 205, 51, 115, 128, 34);
                preencher(g, p, c1);
                g.setColor(c2);
                g.fill(new Ellipse2D.Double(98, 96, 28, 50));
                pontos(g, p, c3, 10, 4, r);
                break;
            }
            case "halfmoon": {
                Shape s = new Arc2D.Double(28, 70, 200, 200, 0, 180, Arc2D.CHORD);
                preencher(g, s, c1);
                g.setColor(c3);
                g.fill(new Arc2D.Double(70, 120, 116, 110, 0, 180, Arc2D.CHORD));
                g.setColor(c2);
                for (int x = 36; x <= 220; x += 14) {
                    g.fill(new Ellipse2D.Double(x, 200, 8, 8));
                }
                break;
            }
            case "round": {
                Shape s = new Ellipse2D.Double(50, 60, 156, 150);
                g.setPaint(new GradientPaint(80, 70, c2, 190, 200, c1));
                g.fill(s);
                contorno(g, s, c1);
                pontos(g, s, c3, 12, 7, r);
                break;
            }
            case "esfiha": {
                Shape s = new Ellipse2D.Double(44, 70, 168, 140);
                preencher(g, s, c1);
                Shape topo = new Ellipse2D.Double(70, 88, 116, 92);
                preencher(g, topo, c2);
                pontos(g, topo, c3, 8, 6, r);
                break;
            }
            case "oval": {
                Shape s = new Ellipse2D.Double(34, 76, 188, 112);
                g.setPaint(new GradientPaint(60, 80, c2, 200, 190, c1));
                g.fill(s);
                contorno(g, s, c1);
                g.setColor(c3);
                g.setStroke(new BasicStroke(3f));
                for (int x = 70; x < 200; x += 22) {
                    g.drawLine(x, 100, x + 12, 164);
                }
                break;
            }
            case "roll": {
                Shape s = new RoundRectangle2D.Double(34, 96, 188, 70, 70, 70);
                preencher(g, s, c1);
                g.setColor(c3);
                g.fill(new Ellipse2D.Double(180, 104, 40, 54));
                g.setColor(c2);
                g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(54, 130, 190, 130);
                break;
            }
            case "sandwich": {
                preencher(g, new Arc2D.Double(46, 52, 164, 120, 0, 180, Arc2D.CHORD), c1);
                preencher(g, new RoundRectangle2D.Double(40, 128, 176, 18, 18, 18), c3);
                preencher(g, new RoundRectangle2D.Double(46, 142, 164, 22, 12, 12), c2);
                preencher(g, new RoundRectangle2D.Double(44, 162, 168, 30, 22, 22), c1);
                g.setColor(c1.brighter());
                for (int x = 90; x <= 170; x += 26) {
                    g.fill(new Ellipse2D.Double(x, 82, 12, 6));
                }
                break;
            }
            case "hotdog": {
                preencher(g, new RoundRectangle2D.Double(30, 110, 196, 70, 60, 60), c1);
                preencher(g, new RoundRectangle2D.Double(24, 100, 208, 30, 30, 30), c2);
                g.setColor(c3);
                g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D z = new Path2D.Double();
                z.moveTo(50, 112);
                for (int x = 70; x <= 210; x += 20) {
                    z.lineTo(x, (x / 20) % 2 == 0 ? 104 : 120);
                }
                g.draw(z);
                break;
            }
            case "bread": {
                Path2D p = new Path2D.Double();
                p.moveTo(52, 190);
                p.lineTo(52, 100);
                p.curveTo(40, 52, 110, 46, 128, 62);
                p.curveTo(146, 46, 216, 52, 204, 100);
                p.lineTo(204, 190);
                p.closePath();
                preencher(g, p, c1);
                preencher(g, new RoundRectangle2D.Double(72, 86, 112, 88, 24, 24), c2);
                g.setColor(c3);
                g.fill(new Ellipse2D.Double(100, 120, 56, 14));
                break;
            }
            case "slice": {
                Path2D p = new Path2D.Double();
                p.moveTo(40, 190);
                p.lineTo(60, 96);
                p.lineTo(216, 120);
                p.lineTo(216, 190);
                p.closePath();
                preencher(g, p, c1);
                preencher(g, new RoundRectangle2D.Double(58, 92, 160, 22, 20, 20), c2);
                g.setColor(c3);
                g.fill(new RoundRectangle2D.Double(46, 148, 170, 10, 8, 8));
                g.setColor(c1.brighter());
                for (int x = 90; x < 200; x += 36) {
                    g.fill(new Ellipse2D.Double(x, 164, 10, 10));
                }
                break;
            }
            case "ball": {
                preencher(g, new RoundRectangle2D.Double(70, 150, 116, 48, 16, 16), c3);
                Shape s = new Ellipse2D.Double(66, 64, 124, 118);
                g.setPaint(new GradientPaint(80, 70, c2, 180, 180, c1));
                g.fill(s);
                contorno(g, s, c1);
                pontos(g, s, c3, 26, 4, r);
                break;
            }
            case "cookie": {
                Shape s = new Ellipse2D.Double(44, 56, 168, 158);
                g.setPaint(new GradientPaint(70, 70, c3, 190, 200, c1));
                g.fill(s);
                contorno(g, s, c1);
                pontos(g, s, c2, 9, 11, r);
                break;
            }
            case "donut": {
                Shape s = new Ellipse2D.Double(36, 56, 184, 158);
                preencher(g, s, c3);
                Shape glace = new Ellipse2D.Double(46, 62, 164, 140);
                preencher(g, glace, c1);
                g.setColor(new Color(0xFBE0EA));
                g.fill(new Ellipse2D.Double(104, 102, 48, 42));
                contorno(g, new Ellipse2D.Double(104, 102, 48, 42), c3);
                Color[] confete = {c2, new Color(0x4FC3F7), new Color(0xFFD54F), new Color(0x81C784)};
                for (int i = 0; i < 18; i++) {
                    double a = r.nextDouble() * Math.PI * 2, d = 40 + r.nextDouble() * 36;
                    g.setColor(confete[i % confete.length]);
                    g.fill(new RoundRectangle2D.Double(128 + Math.cos(a) * d, 132 + Math.sin(a) * d * 0.85, 10, 4, 4, 4));
                }
                break;
            }
            case "cup": {
                Path2D copo = new Path2D.Double();
                copo.moveTo(72, 70);
                copo.lineTo(184, 70);
                copo.lineTo(170, 202);
                copo.lineTo(86, 202);
                copo.closePath();
                g.setColor(new Color(255, 255, 255, 170));
                g.fill(copo);
                Path2D suco = new Path2D.Double();
                suco.moveTo(76, 100);
                suco.lineTo(180, 100);
                suco.lineTo(170, 202);
                suco.lineTo(86, 202);
                suco.closePath();
                g.setPaint(new GradientPaint(80, 100, c1, 180, 200, c3));
                g.fill(suco);
                contorno(g, copo, c1);
                g.setColor(c3);
                g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(150, 24, 138, 100);
                break;
            }
            case "mug": {
                Shape asa = new Arc2D.Double(160, 98, 60, 64, -90, 180, Arc2D.OPEN);
                g.setColor(c3);
                g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(asa);
                preencher(g, new RoundRectangle2D.Double(60, 84, 112, 112, 24, 24), c2);
                g.setColor(c1);
                g.fill(new RoundRectangle2D.Double(66, 90, 100, 20, 14, 14));
                g.setColor(new Color(0, 0, 0, 60));
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(86, 36, 20, 36, 0, 180);
                g.drawArc(124, 30, 20, 40, 0, 180);
                break;
            }
            case "can": {
                preencher(g, new RoundRectangle2D.Double(84, 54, 88, 146, 14, 14), c1);
                g.setColor(c2);
                g.fill(new Ellipse2D.Double(84, 48, 88, 20));
                g.setColor(c3);
                g.fill(new Ellipse2D.Double(104, 52, 48, 10));
                g.setColor(c2);
                g.fill(new Path2D.Double(new Arc2D.Double(98, 100, 60, 60, 0, 360, Arc2D.OPEN)));
                g.setColor(c1);
                g.fill(new Ellipse2D.Double(110, 112, 36, 36));
                break;
            }
            case "bottle": {
                Path2D b = new Path2D.Double();
                b.moveTo(112, 40);
                b.lineTo(144, 40);
                b.lineTo(144, 76);
                b.curveTo(176, 96, 176, 110, 176, 130);
                b.lineTo(176, 196);
                b.quadTo(176, 206, 166, 206);
                b.lineTo(90, 206);
                b.quadTo(80, 206, 80, 196);
                b.lineTo(80, 130);
                b.curveTo(80, 110, 80, 96, 112, 76);
                b.closePath();
                g.setPaint(new GradientPaint(80, 100, c1, 180, 200, c3));
                g.fill(b);
                contorno(g, b, c3);
                g.setColor(c3);
                g.fill(new RoundRectangle2D.Double(108, 26, 40, 18, 6, 6));
                g.setColor(c2);
                g.fill(new RoundRectangle2D.Double(80, 140, 96, 40, 6, 6));
                g.setColor(c3);
                g.fill(new Ellipse2D.Double(116, 150, 24, 20));
                break;
            }
            default:
                throw new IllegalArgumentException(tipo);
        }
    }
}
