package net.onelitefeather.vulpes.generator.release.base;

import net.onelitefeather.vulpes.generator.git.BaseCheckout;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason;
import net.onelitefeather.vulpes.generator.release.codegen.CodegenGate;
import net.onelitefeather.vulpes.generator.release.codegen.GeneratedFile;
import net.onelitefeather.vulpes.generator.release.compile.CompilationGate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the base step with a local origin, a fake base build and a fake code generator. No network, no Gradle.
 *
 * @author TheMeinerLP
 */
class BaseVerificationTest {

    private static final String SWORD = "package items;\n\npublic final class Sword {\n}\n";

    @TempDir
    Path tempDir;

    @Test
    void successfulVerificationReturnsTheBranchAndItsFullCommit() throws Exception {
        LocalBaseOrigin.Origin origin = LocalBaseOrigin.create(tempDir.resolve("origin"));
        BaseVerification verification = verification(origin, new FakeBaseBuild(), sourceOf(SWORD));

        BaseVerification.VerifiedBase verified = verification.verify(input());

        assertEquals("develop", verified.branch());
        assertEquals(origin.head(), verified.commit(), "the commit must be the full SHA of the branch head");
    }

    @Test
    void releaseJsonIsBuiltFromTheResolvedCommit() throws Exception {
        LocalBaseOrigin.Origin origin = LocalBaseOrigin.create(tempDir.resolve("origin"));
        List<String> seen = new ArrayList<>();
        BaseVerification verification = verification(origin, new FakeBaseBuild(), sourceOf(SWORD));

        verification.verify(new BaseVerification.Input("develop", "example", "example.pack",
                "{}".getBytes(StandardCharsets.UTF_8), commit -> {
            seen.add(commit);
            return ("{\"base\":\"" + commit + "\"}").getBytes(StandardCharsets.UTF_8);
        }));

        assertEquals(List.of(origin.head()), seen, "release.json must name the commit that was compiled");
    }

    @Test
    void sameCommitIsBuiltOnlyOnceAcrossVerifications() throws Exception {
        LocalBaseOrigin.Origin origin = LocalBaseOrigin.create(tempDir.resolve("origin"));
        FakeBaseBuild build = new FakeBaseBuild();
        BaseVerification verification = verification(origin, build, sourceOf(SWORD));

        verification.verify(input());
        verification.verify(input());

        assertEquals(1, build.builds(), "the base of one commit is built once");
    }

    @Test
    void compilationFailureGivesCompilationFailedWithDiagnostics() throws Exception {
        LocalBaseOrigin.Origin origin = LocalBaseOrigin.create(tempDir.resolve("origin"));
        String broken = "package items;\n\npublic final class Sword {\n    int damage = \"twelve\";\n}\n";
        BaseVerification verification = verification(origin, new FakeBaseBuild(), sourceOf(broken));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> verification.verify(input()));

        assertEquals(Reason.COMPILATION_FAILED, e.getReason());
        assertEquals(1, e.getDiagnostics().size());
        assertEquals(4, e.getDiagnostics().getFirst().line());
    }

    @Test
    void unknownBranchInTheCloneGivesBaseUnavailableAndBuildsNothing() throws Exception {
        LocalBaseOrigin.Origin origin = LocalBaseOrigin.create(tempDir.resolve("origin"));
        FakeBaseBuild build = new FakeBaseBuild();
        BaseVerification verification = verification(origin, build, sourceOf(SWORD));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> verification.verify(new BaseVerification.Input("no-such-branch", "example", "example.pack",
                        new byte[0], commit -> new byte[0])));

        assertEquals(Reason.BASE_UNAVAILABLE, e.getReason());
        assertEquals(0, build.builds());
    }

    @Test
    void noRequestDirectoryOrCloneRemainsAfterSuccess() throws Exception {
        LocalBaseOrigin.Origin origin = LocalBaseOrigin.create(tempDir.resolve("origin"));
        BaseVerification verification = verification(origin, new FakeBaseBuild(), sourceOf(SWORD));

        verification.verify(input());

        assertOnlyBaseCacheRemains();
    }

    @Test
    void noRequestDirectoryOrCloneRemainsAfterCompilationFailure() throws Exception {
        LocalBaseOrigin.Origin origin = LocalBaseOrigin.create(tempDir.resolve("origin"));
        String broken = "package items;\n\npublic final class Sword {\n    int damage = \"twelve\";\n}\n";
        BaseVerification verification = verification(origin, new FakeBaseBuild(), sourceOf(broken));

        assertThrows(ReleaseRejectedException.class, () -> verification.verify(input()));

        assertOnlyBaseCacheRemains();
    }

    private void assertOnlyBaseCacheRemains() throws Exception {
        try (Stream<Path> children = Files.list(tempDir.resolve("work"))) {
            assertTrue(children.map(path -> path.getFileName().toString()).allMatch("base-cache"::equals),
                    "only the base cache may remain in the work directory");
        }
    }

    private BaseVerification verification(LocalBaseOrigin.Origin origin, FakeBaseBuild build,
                                          net.onelitefeather.vulpes.generator.release.codegen.CodegenPort codegen) {
        Path work = tempDir.resolve("work");
        BaseCheckout checkout = new BaseCheckout(origin.url(), Duration.ofMinutes(2), work);
        BaseClassCache cache = new BaseClassCache(build, work.resolve("base-cache"), 3,
                new AdvancingClock());
        return new BaseVerification(checkout, cache, new CodegenGate(codegen), new CompilationGate(),
                work.toString());
    }

    private BaseVerification.Input input() {
        return new BaseVerification.Input("develop", "example", "example.pack", new byte[0],
                commit -> "{}".getBytes(StandardCharsets.UTF_8));
    }

    /** A generator that writes one source file with the given content. */
    private static net.onelitefeather.vulpes.generator.release.codegen.CodegenPort sourceOf(String content) {
        return (releaseJson, snapshotJson, packageName, projectName) ->
                List.of(new GeneratedFile("items/Sword.java", content));
    }
}
