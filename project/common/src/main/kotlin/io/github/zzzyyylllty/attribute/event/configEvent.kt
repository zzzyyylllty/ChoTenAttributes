package io.github.zzzyyylllty.attribute.event

import io.github.zzzyyylllty.attribute.data.AttributeType
import io.github.zzzyyylllty.attribute.data.RegistrationSource
import taboolib.platform.type.BukkitProxyEvent

class ChoTenAttributeReloadEvent() : BukkitProxyEvent()

/**
 * Modify [defaultData] you can register
 * your custom utils.
 * DO NOT USE clear, re-set or directly modify it, OR OTHER SENSITIVE FUNCTIONS.
 * */
class ChoTenAttributeCustomScriptDataLoadEvent(
    var defaultData: LinkedHashMap<String, Any?>
) : BukkitProxyEvent()

/**
 * 属性注册事件，当一个新属性被注册时触发。
 * 其他插件可监听此事件，在属性注册时执行自定义逻辑。
 */
class ChoTenAttributeRegisterEvent(
    val attributeType: AttributeType,
    val source: RegistrationSource
) : BukkitProxyEvent()

