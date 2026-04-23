package io.github.zzzyyylllty.attribute.util//package io.github.zzzyyylllty.attribute.util
//
//import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
//import net.momirealms.craftengine.core.util.Key
//import org.bukkit.Location
//import org.bukkit.Material
//import org.bukkit.block.Block
//import org.bukkit.block.data.BlockData
//import taboolib.common.LifeCycle
//import taboolib.common.platform.Awake
//import kotlin.collections.iterator
//
//val blockAdapters = LinkedHashMap<String, BlockAdapter>()
//
//data class MultiBlock(
//    val adapter: String,
//    val block: String,
//)
//
//fun getBlockFromKey(input: String): MultiBlock {
//    if (!input.contains(":")) return MultiBlock("minecraft", input.uppercase())
//    val split = input.split(":".toRegex(), 2)
//    return MultiBlock(split[0], input.removePrefix("${split[0]}:"))
//}
//fun getBlockID(block: Block): MultiBlock {
//    if (blockAdapters.isNotEmpty()) {
//        for (a in blockAdapters) {
//            a.value.getID(block)?.let { return it }
//        }
//    }
//    return MCAdapter.getID(block)
//}
//
//public interface BlockAdapter {
//    fun getID(block: Block): MultiBlock?
//    fun place(block: Block, blockData: BlockData, location: Location, id: String)
//    fun placeByID(location: Location, id: String)
//}
//fun HashSet<MultiBlock>.validate(block: Block): Boolean {
//    val mBlock = getBlockID(block)
//    return this.contains(mBlock)
//}
//fun HashSet<String>.loadBlocks(): HashSet<MultiBlock> {
//    val set = HashSet<MultiBlock>()
//    for (block in this) {
//        set.add(getBlockFromKey(block))
//    }
//    return set
//}
//object CEAdapter : BlockAdapter {
//    override fun getID(block: Block): MultiBlock? {
//        return CraftEngineBlocks.getCustomBlockState(block)?.customBlockState()?.ownerId()?.asString()?.let { MultiBlock("craftengine", it) }
//    }
//    override fun place(block: Block, blockData: BlockData, location: Location, id: String) {
//        CraftEngineBlocks.getCustomBlockState(block)?.let { CraftEngineBlocks.place(location, it, false) }
//    }
//    override fun placeByID(location: Location, id: String) {
//        CraftEngineBlocks.place(location, Key.of(id), false)
//    }
//
//}
//object MCAdapter : BlockAdapter {
//    override fun getID(block: Block): MultiBlock {
//        return MultiBlock("minecraft", block.type.name)
//    }
//    override fun place(block: Block, blockData: BlockData, location: Location, id: String) {
//        location.world.getBlockAt(location).type = Material.valueOf(id)
//    }
//    override fun placeByID(location: Location, id: String) {
//        location.world.getBlockAt(location).type = Material.valueOf(id)
//    }
//}
//
//@Awake(LifeCycle.ENABLE)
//fun registerNativeBlockAdapters() {
//    if (DependencyHelper.ce) blockAdapters["craftengine"] = CEAdapter
//    blockAdapters["minecraft"] = MCAdapter
//}
