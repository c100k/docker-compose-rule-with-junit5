import com.palantir.docker.compose.DockerComposeRule;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static com.palantir.docker.compose.connection.waiting.HealthChecks.toHaveAllPortsOpen;

public class DockerComposeExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private DockerComposeRule docker;

    public DockerComposeExtension() {
        docker = DockerComposeRule.builder()
                .pullOnStartup(true)
                .file("src/test/resources/docker-compose.yml")
                .saveLogsTo("target/test-docker-logs")
                .waitingForService("postgres", toHaveAllPortsOpen())
                .waitingForService("nginx", toHaveAllPortsOpen())
                .build();
    }

    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        docker.before();
    }

    public void afterAll(ExtensionContext extensionContext) throws Exception {
        docker.after();
    }

    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(DockerComposeRule.class);
    }

    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return docker;
    }

}
