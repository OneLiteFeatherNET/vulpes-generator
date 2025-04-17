package net.theevilreaper.vulpes.generator.domain.pack;

/**
 * The {@link PackOption} enumeration contains all available modes for the resource pack generation.
 * The idea behind this is to allow the limitation of the resource pack generation to only the needed files.
 * This is helpfully when the user wants to test a specific feature of the resource pack.
 * <p>
 * Explanation of the options:
 * - {@link #ALL} - All files will be generated.
 * - {@link #FONT} - Only the font files will be generated.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public enum PackOption {

    ALL,
    FONT,
}
