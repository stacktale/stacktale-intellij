package io.github.gabrielbbaldez.stacktale.idea;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StReportParserTest {

    private static final String SAMPLE = """
            # AI-oriented error reports (format st/1, https://github.com/stacktale/stacktale)
            # Each report is delimited by "━━━ ERROR #<id> ━━━" ... "━━━ END #<id> ━━━".
            ━━━ ERROR #a1b2c3d4 ━━━ 2026-07-10 20:16:40.412 thread=http-nio-8080-exec-2 ━━━
            IllegalStateException: payment gateway refused
            at PaymentService.charge(PaymentService.java:44) ← YOUR CODE
            log: "charge failed for order {}" args=[889] logger=c.a.s.PaymentService

            stack (distilled, 1 of 32 frames):
              PaymentService.charge(PaymentService.java:44) ← culprit

            env: app=shop-api | java 21 | linux
            ━━━ END #a1b2c3d4 ━━━
            """;

    @Test
    void parsesReportIdHeadlineAndCulpritLocation() {
        List<StReport> reports = StReportParser.parse(SAMPLE);

        assertThat(reports).hasSize(1);
        StReport r = reports.get(0);
        assertThat(r.id()).isEqualTo("a1b2c3d4");
        assertThat(r.timestamp()).isEqualTo("2026-07-10 20:16:40.412");
        assertThat(r.headline()).isEqualTo("IllegalStateException: payment gateway refused");
        assertThat(r.culprit()).isNotNull();
        assertThat(r.culprit().fileName()).isEqualTo("PaymentService.java");
        assertThat(r.culprit().line()).isEqualTo(44);
        assertThat(r.block()).startsWith("━━━ ERROR #a1b2c3d4").contains("━━━ END #a1b2c3d4");
    }

    @Test
    void ignoresTheSelfDescribingHeaderAndParsesEveryReport() {
        String two = SAMPLE
                + "━━━ ERROR #beef ━━━ 2026-07-10 20:17:00.000 thread=main ━━━\n"
                + "NullPointerException: customer is null\n"
                + "at OrderService.confirm(OrderService.java:87) ← YOUR CODE\n"
                + "━━━ END #beef ━━━\n";

        List<StReport> reports = StReportParser.parse(two);

        assertThat(reports).extracting(StReport::id).containsExactly("a1b2c3d4", "beef");
        assertThat(reports.get(1).culprit().line()).isEqualTo(87);
    }

    @Test
    void aTruncatedTrailingBlockIsStillSurfaced() {
        // a file killed mid-write: the last block has no END line
        String truncated = "━━━ ERROR #dead ━━━ 2026-07-10 20:18:00.000 thread=main ━━━\n"
                + "RuntimeException: boom\n"
                + "at Svc.run(Svc.java:12) ← YOUR CODE\n";

        List<StReport> reports = StReportParser.parse(truncated);

        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).culprit().fileName()).isEqualTo("Svc.java");
    }

    @Test
    void toleratesEmptyAndHeaderOnlyFiles() {
        assertThat(StReportParser.parse("")).isEmpty();
        assertThat(StReportParser.parse("# just a header\n# no reports yet\n")).isEmpty();
    }
}
