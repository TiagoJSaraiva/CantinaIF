package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

import utils.Database;

/** Banco H2 em memoria isolado por classe de teste. */
final class TestDb {

    private TestDb() {
    }

    static void start(Class<?> teste) {
        Database.shutdown();
        Database.init("jdbc:h2:mem:t_" + teste.getSimpleName()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", null, null);
    }

    static void stop() {
        Database.shutdown();
    }

    /** Cria um vendedor (se ainda nao existir) e devolve o prontuario. */
    static String vendedor(String prontuario) throws Exception {
        new VendedorDao().create(prontuario, "Cantina " + prontuario, prontuario.toLowerCase() + "@v.test", "senha-vendedor");
        return prontuario;
    }

    /** Liga o comprador ao vendedor direto no banco (o fluxo por codigo e testado em VinculoDaoTest). */
    static void vincular(String comprador, String vendedor) throws Exception {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE Comprador SET vendedor_prontuario = ? WHERE prontuario = ?")) {
            ps.setString(1, vendedor);
            ps.setString(2, comprador);
            ps.executeUpdate();
        }
    }
}
