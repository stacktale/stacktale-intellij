package io.github.gabrielbbaldez.stacktale.idea;

/** A source location parsed from a report frame — enough to navigate to it in the editor. */
public record StFrame(String fileName, int line, String raw) {
}
