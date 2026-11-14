package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.onelitefeather.vulpes.generator.release.PackTestData.CONTENTS;
import static net.onelitefeather.vulpes.generator.release.PackTestData.SNAPSHOT;
import static net.onelitefeather.vulpes.generator.release.PackTestData.SOUND;
import static net.onelitefeather.vulpes.generator.release.PackTestData.SOUND_ID;
import static net.onelitefeather.vulpes.generator.release.PackTestData.SOUND_PATH;
import static net.onelitefeather.vulpes.generator.release.PackTestData.TEXTURE;
import static net.onelitefeather.vulpes.generator.release.PackTestData.TEXTURE_ID;
import static net.onelitefeather.vulpes.generator.release.PackTestData.TEXTURE_PATH;
import static net.onelitefeather.vulpes.generator.release.PackTestData.asset;
import static net.onelitefeather.vulpes.generator.release.PackTestData.inputs;
import static net.onelitefeather.vulpes.generator.release.PackTestData.meta;
import static net.onelitefeather.vulpes.generator.release.PackTestData.sha1;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.PACK_STORAGE_UNAVAILABLE;
import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the resource pack service: the asset bytes it reads and checks, the key and entry of a prepared pack, and the
 * upload to the bucket. The backend and the bucket are in-process fakes.
 *
 * @author TheMeinerLP
 */
class ResourcePackServiceTest {

    private static final String PROJECT_KEY = "cygnus";

    private final FakeAssetContent assets = new FakeAssetContent();
    private final List<String> events = new ArrayList<>();
    private final InMemoryPackStorage storage = new InMemoryPackStorage(events);

    @Test
    void assetBytesAreReadOncePerAsset() {
        assets.contents.putAll(CONTENTS);
        ResourcePackInputs inputs = new ResourcePackInputs(meta("Cygnus"), List.of(
                asset(TEXTURE_PATH, TEXTURE_ID, TEXTURE),
                asset("assets/cygnus/textures/font/copy.png", TEXTURE_ID, TEXTURE),
                asset(SOUND_PATH, SOUND_ID, SOUND)));

        service(Optional.of(storage)).build(PROJECT_ID, snapshot(inputs));

        assertEquals(List.of(TEXTURE_ID, SOUND_ID), assets.reads,
                "an asset that the pack uses twice must be read once, in the order of the pack inputs");
    }

    @Test
    void assetWithAnotherSha1IsRefusedNamingTheAsset() {
        assets.contents.put(TEXTURE_ID, "changed".getBytes(StandardCharsets.UTF_8));
        ResourcePackInputs inputs = new ResourcePackInputs(meta("Cygnus"), List.of(asset(TEXTURE_PATH, TEXTURE_ID, TEXTURE)));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service(Optional.of(storage)).build(PROJECT_ID, snapshot(inputs)));

        assertEquals(SOURCE_UNAVAILABLE, e.getReason(), "a changed asset must refuse the release as SOURCE_UNAVAILABLE");
        assertTrue(e.getMessage().contains(TEXTURE_PATH), "the refusal must name the asset: " + e.getMessage());
    }

    @Test
    void failedAssetReadIsRefusedNamingTheAsset() {
        assets.failWith = new ReleaseRejectedException(SOURCE_UNAVAILABLE, "backend answered 503");
        ResourcePackInputs inputs = new ResourcePackInputs(meta("Cygnus"), List.of(asset(TEXTURE_PATH, TEXTURE_ID, TEXTURE)));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service(Optional.of(storage)).build(PROJECT_ID, snapshot(inputs)));

        assertEquals(SOURCE_UNAVAILABLE, e.getReason(), "a failed read must refuse the release as SOURCE_UNAVAILABLE");
        assertTrue(e.getMessage().contains(TEXTURE_PATH), "the refusal must name the asset: " + e.getMessage());
    }

    @Test
    void buildGivesNoZipWithoutPackSettings() {
        Optional<byte[]> zip = service(Optional.of(storage)).build(PROJECT_ID,
                snapshot(new ResourcePackInputs(null, List.of())));

        assertTrue(zip.isEmpty(), "a project without pack settings has no resource pack");
        assertTrue(assets.reads.isEmpty(), "no asset may be read for a project without pack settings");
    }

    @Test
    void prepareStoresTheZipUnderTheProjectKeyAndItsSha1() {
        assets.contents.putAll(CONTENTS);

        ResourcePackService.PreparedPack pack = service(Optional.of(storage))
                .prepare(PROJECT_ID, snapshot(inputs(meta("Cygnus")))).orElseThrow();

        String sha1 = sha1(pack.zip());
        assertEquals("vulpes/cygnus/" + sha1 + ".zip", pack.key(), "the key must be vulpes/<project>/<sha1>.zip");
        assertEquals(new ReleasePack("cygnus", 16, storage.url(pack.key()), sha1, pack.zip().length), pack.entry(),
                "the release entry must carry the id, resolution 16, the public URL, the SHA-1 and the size of the zip");
    }

    @Test
    void sameContentGivesTheSameKey() {
        assets.contents.putAll(CONTENTS);
        ResourcePackService service = service(Optional.of(storage));

        String first = service.prepare(PROJECT_ID, snapshot(inputs(meta("Cygnus")))).orElseThrow().key();
        String second = service.prepare(PROJECT_ID, snapshot(inputs(meta("Cygnus")))).orElseThrow().key();

        assertEquals(first, second, "the same content must always address the same object in the bucket");
    }

    @Test
    void prepareGivesNothingWithoutStorageAndReadsNoAsset() {
        Optional<ResourcePackService.PreparedPack> pack = service(Optional.empty())
                .prepare(PROJECT_ID, snapshot(inputs(meta("Cygnus"))));

        assertTrue(pack.isEmpty(), "without a configured bucket no pack is prepared for a release");
        assertTrue(assets.reads.isEmpty(), "without a configured bucket no asset is read for a release");
    }

    @Test
    void prepareGivesNothingWithoutPackSettings() {
        Optional<ResourcePackService.PreparedPack> pack = service(Optional.of(storage))
                .prepare(PROJECT_ID, snapshot(new ResourcePackInputs(null, List.of())));

        assertTrue(pack.isEmpty(), "a project without pack settings lists no pack");
    }

    @Test
    void uploadStoresTheZipUnderItsKey() {
        assets.contents.putAll(CONTENTS);
        ResourcePackService service = service(Optional.of(storage));
        ResourcePackService.PreparedPack pack = service.prepare(PROJECT_ID, snapshot(inputs(meta("Cygnus")))).orElseThrow();

        service.upload(pack);

        assertArrayEquals(pack.zip(), storage.get(pack.key()), "the bucket must hold the zip under its key");
        assertEquals(List.of("put " + pack.key()), events, "the zip must be uploaded once");
    }

    @Test
    void uploadOfAnExistingKeyIsSkipped() {
        assets.contents.putAll(CONTENTS);
        ResourcePackService service = service(Optional.of(storage));
        ResourcePackService.PreparedPack pack = service.prepare(PROJECT_ID, snapshot(inputs(meta("Cygnus")))).orElseThrow();
        storage.seed(pack.key(), pack.zip());

        service.upload(pack);

        assertEquals(0, storage.putCount, "an object that already exists under its key must not be uploaded again");
    }

    @Test
    void failedUploadIsRefusedWithPackStorageUnavailable() {
        assets.contents.putAll(CONTENTS);
        storage.failPuts = true;
        ResourcePackService service = service(Optional.of(storage));
        ResourcePackService.PreparedPack pack = service.prepare(PROJECT_ID, snapshot(inputs(meta("Cygnus")))).orElseThrow();

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> service.upload(pack));

        assertEquals(PACK_STORAGE_UNAVAILABLE, e.getReason(), "a failed upload must refuse the release as PACK_STORAGE_UNAVAILABLE");
    }

    private ResourcePackService service(Optional<PackStorage> bucket) {
        return new ResourcePackService(assets, bucket);
    }

    private static AssembledSnapshot snapshot(ResourcePackInputs inputs) {
        return new AssembledSnapshot(PROJECT_KEY, SNAPSHOT.getBytes(StandardCharsets.UTF_8), inputs);
    }
}
