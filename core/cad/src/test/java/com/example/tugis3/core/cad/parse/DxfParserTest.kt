package com.example.tugis3.core.cad.parse

import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream

class DxfParserTest {

    @Test
    fun parseSampleDxf_entitiesRecognized() {
        val sample = """
0
SECTION
2
ENTITIES
0
LINE
10
0.0
20
0.0
11
10.0
21
0.0
0
LINE
10
10.0
20
0.0
11
10.0
21
5.0
0
CIRCLE
10
5.0
20
2.5
40
1.0
0
LWPOLYLINE
70
1
10
0.0
20
5.0
10
10.0
20
5.0
10
10.0
20
10.0
10
0.0
20
10.0
0
ENDSEC
0
EOF
""".trimIndent()
        val parser = DxfParser()
        val entities = parser.parse(ByteArrayInputStream(sample.toByteArray()))
        assertEquals(4, entities.size)
    }
}
