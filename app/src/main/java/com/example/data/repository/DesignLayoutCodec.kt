package com.example.data.repository

import com.example.domain.model.LayoutNode
import com.example.domain.model.SplitAxis
import org.json.JSONObject

/** Stored as optional metadata in panelsJson, keeping old databases/backups readable. */
object DesignLayoutCodec {
  fun encode(node: LayoutNode): JSONObject = JSONObject().apply {
    if (node.panelId != null) put("panelId", node.panelId) else {
      put("axis", node.axis.name)
      put("fraction", node.fraction.toDouble())
      put("first", encode(requireNotNull(node.first)))
      put("second", encode(requireNotNull(node.second)))
    }
  }

  fun decode(obj: JSONObject, depth: Int = 0): LayoutNode {
    require(depth < 64) { "Layout is too deep" }
    if (obj.has("panelId")) return LayoutNode(panelId = obj.getString("panelId"))
    val fraction = obj.getDouble("fraction").toFloat()
    require(fraction.isFinite() && fraction > 0 && fraction < 1)
    return LayoutNode(axis = SplitAxis.valueOf(obj.getString("axis")), fraction = fraction,
      first = decode(obj.getJSONObject("first"), depth + 1), second = decode(obj.getJSONObject("second"), depth + 1))
  }
}
