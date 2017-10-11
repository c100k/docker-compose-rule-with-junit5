import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.DockerPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.mashape.unirest.http.Unirest.get;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DockerComposeExtension.class)
public class SimpleTest {

    @Test
    public void shouldConnectToDatabase(DockerComposeRule docker) throws SQLException, ClassNotFoundException {
        // Given
        // We need to import the driver https://jdbc.postgresql.org/documentation/head/load.html
        Class.forName("org.postgresql.Driver");

        DockerPort container = docker.containers().container("postgres").port(5432);
        String url = "jdbc:postgresql://" + container.getIp() + ":" + container.getExternalPort() + "/postgres";
        Connection connection = DriverManager.getConnection(url, "postgres", "ThisIsMySuperPassword");

        // When
        ResultSet resultSet = connection.prepareStatement("SELECT 1").executeQuery();

        // Then
        assertNotNull(resultSet);
    }

    @Test
    public void shouldReceiveNginxWelcome(DockerComposeRule docker) throws UnirestException {
        // Given
        DockerPort container = docker.containers().container("nginx").port(80);
        String url = "http://" + container.getIp() + ":" + container.getExternalPort();

        // When
        HttpResponse<String> response = get(url).asString();

        // Then
        assertTrue(response.getBody().contains("Welcome to nginx!"));
    }

}
