package org.hiatusuk.obsidian.benchmarks

import org.hiatusuk.obsidian.utils.IOUtils
import org.openjdk.jmh.annotations.*

import java.util.LinkedHashMap
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
open class CloneBenchmarks {

    private val map = testMap

    //    @Benchmark
    //    public Object testSlowIoDeepClone() {
    //        return IOUtils.deepClone(map);
    //    }
    //
    //    public static Object deepClone(final Object o) {
    //        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream()) {
    //            try (ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
    //                out.writeObject(o);
    //                out.flush();
    //                try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream( byteOut.toByteArray() ))) {
    //                    return o.getClass().cast(in.readObject());
    //                }
    //            }
    //        }
    //        catch (ClassNotFoundException | IOException e) {
    //            throw new RuntimeException(e);
    //        }
    //    }

    private val testMap: Map<Any?, Any>
        get() {
            val inner = LinkedHashMap<String, Any>()
            inner["that"] = "\${math:min(b,1900)} - \${math:max(b,1900)}, diff = \${math:max(c,1900) - math:min(c,1900)}, eq=1900 - 1979, diff = 79}"

            val m = LinkedHashMap<Any?, Any>()
            m["assert"] = inner
            return m
        }

    @Benchmark
    fun testMapDeepClone(): Any {
        return IOUtils.deepCloneMap(map)
    }
}
