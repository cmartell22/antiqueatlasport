package folk.sisby.roleplayers_atlas.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import folk.sisby.roleplayers_atlas.RoleplayersAtlas;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Replicates the behaviour of the pre-1.21.5 gson-based JsonDataLoader:
 * collects every json under the given directory into a raw JsonElement map,
 * leaving the parsing to the subclass.
 */
public abstract class LegacyJsonDataLoader extends SinglePreparationResourceReloader<Map<Identifier, JsonElement>> {
	private final ResourceFinder finder;

	protected LegacyJsonDataLoader(String dataType) {
		this.finder = ResourceFinder.json(dataType);
	}

	@Override
	protected Map<Identifier, JsonElement> prepare(ResourceManager manager, Profiler profiler) {
		Map<Identifier, JsonElement> map = new HashMap<>();
		for (Map.Entry<Identifier, Resource> entry : finder.findResources(manager).entrySet()) {
			Identifier id = finder.toResourceId(entry.getKey());
			try (Reader reader = entry.getValue().getReader()) {
				map.put(id, JsonParser.parseReader(reader));
			} catch (Exception e) {
				RoleplayersAtlas.LOGGER.error("[Roleplayer's Atlas] Couldn't parse data file {} from {}", id, entry.getKey(), e);
			}
		}
		return map;
	}
}
