package me.earth.headlessmc.launcher.instrumentation;

import lombok.Cleanup;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.val;
import me.earth.headlessmc.launcher.util.IOUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

@CustomLog
@RequiredArgsConstructor
public class Instrumentation {
    private final List<Transformer> transformers;
    private final File base;

    public List<String> instrument(List<Target> targetsIn) throws IOException {
        List<Target> targets = targetsIn;
        if (transformers.isEmpty()) {
            return targets.stream()
                          .map(Target::getPath)
                          .collect(Collectors.toList());
        }

        for (val transformer : transformers) {
            targets = transformer.transform(targets);
        }

        val result = new ArrayList<String>(targets.size());
        for (val target : targets) {
            val targetTransformers = transformers
                .stream()
                .filter(transformer -> transformer.matches(target))
                .collect(Collectors.toList());

            val url = runTransformers(target, targets, targetTransformers);
            result.add(url);
        }

        val inactive = getInactiveTransformers(transformers);
        if (!inactive.isEmpty()) {
            log.info("Transformers did not run: " + inactive);
        }

        return result;
    }

    // TODO: this is too long and kinda ugly
    private String runTransformers(Target target,
                                   List<Target> targets,
                                   List<Transformer> transformers)
        throws IOException {
        if (transformers.isEmpty()) {
            return target.getPath();
        }

        @Cleanup
        val jar = target.toJar();
        // TODO: cache????
        val file = base.getAbsolutePath()
            + File.separator
            + new File(jar.getName()).getName();
        File targetJar = new File(file);
        if (targetJar.exists()) {
            log.warning(targetJar + " already exists!");
            return file;
        }

        @Cleanup
        val jos = IOUtil.jarOutput(targetJar);
        for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
            val next = e.nextElement();
            @Cleanup
            val is = jar.getInputStream(next);
            EntryStream stream = new EntryStream(is, targets, next);

            for (Transformer transformer : transformers) {
                val stream2 = transformer.transform(stream);
                if (stream2.getStream() != stream.getStream()) {
                    stream.getStream().close();
                }

                stream = stream2;
            }

            if (!stream.isSkipped()) {
                jos.putNextEntry(new JarEntry(next.getName()));
                IOUtil.copy(stream.getStream(), jos);
                jos.flush();
                jos.closeEntry();
            }
        }

        return file;
    }

    private String getInactiveTransformers(List<Transformer> transformers) {
        return transformers.stream()
                           .filter(t -> !t.hasRun())
                           .map(t -> t.getClass().getName())
                           .collect(Collectors.joining(","));
    }

}
