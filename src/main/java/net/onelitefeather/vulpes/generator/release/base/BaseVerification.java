package net.onelitefeather.vulpes.generator.release.base;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.git.BaseCheckout;
import net.onelitefeather.vulpes.generator.release.BuildWorkspace;
import net.onelitefeather.vulpes.generator.release.codegen.CodegenGate;
import net.onelitefeather.vulpes.generator.release.compile.CompilationGate;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * The base step of a local build and of a release: the branch is cloned and resolved to its full commit, the classes
 * of that commit are taken from the cache or built, the snapshot is generated and the sources are compiled against
 * the classes. Nothing is kept afterwards: the clone and the request directory are deleted, and the cache keeps only
 * the classes.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class BaseVerification {

    private final BaseCheckout checkout;
    private final BaseClassCache cache;
    private final CodegenGate codegen;
    private final CompilationGate compiler;
    private final Path workDir;

    /**
     * Creates the verification.
     *
     * @param checkout the shallow clone of the base branch
     * @param cache    the base build cache per commit
     * @param codegen  the code generator gate
     * @param compiler the compilation gate
     * @param workDir  the base directory of the request directories; blank means the JVM temp directory
     */
    @Inject
    public BaseVerification(BaseCheckout checkout, BaseClassCache cache, CodegenGate codegen,
                            CompilationGate compiler, @Value("${vulpes.release.work-dir:}") String workDir) {
        this.checkout = checkout;
        this.cache = cache;
        this.codegen = codegen;
        this.compiler = compiler;
        this.workDir = workDir.isBlank() ? Path.of(System.getProperty("java.io.tmpdir")) : Path.of(workDir);
    }

    /**
     * Verifies a snapshot against a branch of {@code vulpes-base}.
     *
     * @param input the branch, the project and the snapshot to verify
     * @return the branch and the full commit the snapshot was compiled against
     * @throws net.onelitefeather.vulpes.generator.release.ReleaseRejectedException when the branch cannot be cloned or
     *                                  built ({@code BASE_UNAVAILABLE}), the generator refuses the snapshot
     *                                  ({@code GENERATION_REFUSED}) or the sources do not compile
     *                                  ({@code COMPILATION_FAILED})
     */
    public VerifiedBase verify(Input input) {
        return checkout.withCheckout(input.branch(), resolved -> {
            BaseClasses classes = cache.resolve(resolved.commit(), resolved.directory());
            byte[] releaseJson = input.releaseJsonFor().apply(resolved.commit());
            try (BuildWorkspace request = BuildWorkspace.create(workDir)) {
                Path generated = request.dir().resolve("generated");
                codegen.generateInto(generated, releaseJson, input.snapshotJson(), input.packageName(),
                        input.projectName());
                compiler.compile(generated, request.dir().resolve("classes"), classes.classpath());
            }
            return new VerifiedBase(input.branch(), resolved.commit(), releaseJson);
        });
    }

    /**
     * What to verify.
     *
     * @param branch         the {@code vulpes-base} branch, {@code develop} when the caller names none
     * @param projectName    the project name of the snapshot
     * @param packageName    the Java package of the generated sources
     * @param snapshotJson   the snapshot bytes
     * @param releaseJsonFor the {@code release.json} bytes for a full commit SHA; the generator reads them
     */
    public record Input(String branch, String projectName, String packageName, byte[] snapshotJson,
                        Function<String, byte[]> releaseJsonFor) {
    }

    /**
     * The result of a successful verification.
     *
     * @param branch      the branch that was verified
     * @param commit      the full 40-character lower-case SHA-1 the branch resolved to
     * @param releaseJson the {@code release.json} bytes that were compiled against the commit
     */
    public record VerifiedBase(String branch, String commit, byte[] releaseJson) {
    }
}
