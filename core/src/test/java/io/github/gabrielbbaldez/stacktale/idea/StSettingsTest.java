package io.github.gabrielbbaldez.stacktale.idea;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StSettingsTest {

    private static final String BASE = Path.of("home", "dev", "shop-api").toAbsolutePath().toString();

    @Test
    void defaultsReproduceTheBehaviourFromBeforeTheSettingsExisted() {
        assertThat(StSettings.DEFAULT_LOG_PATH).isEmpty();
        assertThat(StSettings.DEFAULT_POLL_SECONDS).isEqualTo(3); // the 3000 ms poll
    }

    @Test
    void blankOrMissingLogPathMeansAutoDetect() {
        assertThat(StSettings.normalizeLogPath(null)).isEqualTo(StSettings.DEFAULT_LOG_PATH);
        assertThat(StSettings.normalizeLogPath("")).isEqualTo(StSettings.DEFAULT_LOG_PATH);
        assertThat(StSettings.normalizeLogPath("   ")).isEqualTo(StSettings.DEFAULT_LOG_PATH);

        assertThat(StSettings.resolveLogPath("", BASE)).isNull();
        assertThat(StSettings.resolveLogPath("  ", BASE)).isNull();
    }

    @Test
    void trimsWhitespaceAroundAPastedLogPath() {
        assertThat(StSettings.normalizeLogPath("  build/errors-ai.log\n")).isEqualTo("build/errors-ai.log");
        assertThat(StSettings.resolveLogPath(" build/errors-ai.log ", BASE))
                .isEqualTo(Path.of(BASE, "build", "errors-ai.log"));
    }

    @Test
    void resolvesARelativeLogPathAgainstTheProjectRoot() {
        assertThat(StSettings.resolveLogPath("build/errors-ai.log", BASE))
                .isEqualTo(Path.of(BASE, "build", "errors-ai.log"));
        assertThat(StSettings.resolveLogPath("./api/build/errors-ai.log", BASE))
                .isEqualTo(Path.of(BASE, "api", "build", "errors-ai.log"));
    }

    @Test
    void keepsAnAbsoluteLogPathAsGiven() {
        Path absolute = Path.of("var", "log", "errors-ai.log").toAbsolutePath();

        assertThat(StSettings.resolveLogPath(absolute.toString(), BASE)).isEqualTo(absolute);
    }

    @Test
    void cannotResolveARelativePathForAProjectWithNoRoot() {
        assertThat(StSettings.resolveLogPath("build/errors-ai.log", null)).isNull();
    }

    @Test
    void rejectsTextTheFilesystemCannotRepresentInsteadOfThrowing() {
        String nulInTheMiddle = "errors" + (char) 0 + "ai.log"; // rejected on every platform

        assertThat(StSettings.resolveLogPath(nulInTheMiddle, BASE)).isNull();
    }

    @Test
    void clampsAPollIntervalOutsideTheSpinnerRange() {
        assertThat(StSettings.clampPollSeconds(0)).isEqualTo(StSettings.MIN_POLL_SECONDS);
        assertThat(StSettings.clampPollSeconds(-30)).isEqualTo(StSettings.MIN_POLL_SECONDS);
        assertThat(StSettings.clampPollSeconds(Integer.MAX_VALUE)).isEqualTo(StSettings.MAX_POLL_SECONDS);
        assertThat(StSettings.clampPollSeconds(StSettings.DEFAULT_POLL_SECONDS))
                .isEqualTo(StSettings.DEFAULT_POLL_SECONDS);
    }
}
