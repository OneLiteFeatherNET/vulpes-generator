package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.release.base.BaseVerification;

import java.util.ArrayList;
import java.util.List;

/**
 * The base verification of the test contexts. It clones and builds nothing: it reports the commit of
 * {@link ReleaseTestData#BASE_COMMIT} and hands the release metadata of that commit to the caller, as the real
 * verification does. {@link #failWith(ReleaseRejectedException)} makes the verification refuse the snapshot.
 *
 * @author TheMeinerLP
 */
@Singleton
@Replaces(BaseVerification.class)
class FakeBaseVerification extends BaseVerification {

    private final List<Input> inputs = new ArrayList<>();
    private ReleaseRejectedException failure;

    FakeBaseVerification() {
        super(null, null, null, null, null, "");
    }

    @Override
    public VerifiedBase verify(Input input) {
        inputs.add(input);
        if (failure != null) {
            throw failure;
        }
        return new VerifiedBase(input.branch(), ReleaseTestData.BASE_COMMIT,
                input.releaseJsonFor().apply(ReleaseTestData.BASE_COMMIT));
    }

    /** The inputs of every verification since the last reset. */
    List<Input> inputs() {
        return List.copyOf(inputs);
    }

    /** Makes the next verifications refuse with the given rejection. */
    void failWith(ReleaseRejectedException failure) {
        this.failure = failure;
    }

    /** Clears the recorded inputs and the failure. */
    void reset() {
        inputs.clear();
        failure = null;
    }
}
