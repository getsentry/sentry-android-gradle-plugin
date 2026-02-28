package io.sentry.android.gradle.snapshot.preview

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonSerializerTest {

  @Test
  fun `serializes empty list`() {
    val json = JsonSerializer.serialize(emptyList())
    assertEquals("[\n]", json)
  }

  @Test
  fun `serializes single config`() {
    val configs =
      listOf(
        PreviewSnapshotConfig(
          displayName = "com.example.Foo.Bar",
          className = "com.example.Foo",
          methodName = "Bar",
        )
      )
    val json = JsonSerializer.serialize(configs)
    assertEquals(
      """
      [
        {"displayName":"com.example.Foo.Bar","className":"com.example.Foo","methodName":"Bar"}
      ]
      """
        .trimIndent(),
      json,
    )
  }

  @Test
  fun `escapes special characters`() {
    val configs =
      listOf(
        PreviewSnapshotConfig(
          displayName = "name with \"quotes\" and \\backslash",
          className = "com.example.Test",
          methodName = "preview",
        )
      )
    val json = JsonSerializer.serialize(configs)
    assert(json.contains("""name with \"quotes\" and \\backslash""")) {
      "Expected escaped quotes and backslash in: $json"
    }
  }

  @Test
  fun `serializes multiple configs with commas`() {
    val configs =
      listOf(PreviewSnapshotConfig("A", "com.A", "a"), PreviewSnapshotConfig("B", "com.B", "b"))
    val json = JsonSerializer.serialize(configs)
    val expected =
      "[\n" +
        "  {\"displayName\":\"A\",\"className\":\"com.A\",\"methodName\":\"a\"},\n" +
        "  {\"displayName\":\"B\",\"className\":\"com.B\",\"methodName\":\"b\"}\n" +
        "]"
    assertEquals(expected, json)
  }
}
