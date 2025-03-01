package net.mt1006.mocap.mocap.playing.modifiers;

import net.mt1006.mocap.command.io.CommandOutput;
import net.mt1006.mocap.mocap.files.Files;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityFilterInstance
{
	public static final String EMPTY_GROUP = "none";
	private final List<Element> elements = new ArrayList<>();
	public final String filterStr;

	private EntityFilterInstance(String str) throws FilterParserException
	{
		filterStr = str;
		String[] parts = str.split(";");

		for (String part : parts)
		{
			if (part.isBlank()) { throw new FilterParserException(); }

			boolean exclude = (part.charAt(0) == '-');
			if (exclude && part.length() == 1) { throw new FilterParserException(); }

			char firstChar = part.charAt(exclude ? 1 : 0);

			if (firstChar == '@')
			{
				String groupName = part.substring(exclude ? 2 : 1);
				if (groupName.equals(EMPTY_GROUP)) { continue; } // "@none" is just ignored
				if (!Files.checkIfProperName(CommandOutput.DUMMY, groupName)) { throw new FilterParserException(); }

				GroupElement groupElement = Group.fromString(exclude, groupName);
				if (groupElement == null) { throw new FilterParserException(); }
				elements.add(groupElement);
			}
			else if (firstChar == '$')
			{
				String tagName = part.substring(exclude ? 2 : 1);
				elements.add(new TagElement(exclude, tagName));
			}
			else
			{
				String name = exclude ? part.substring(1) : part;

				if (name.equals("*"))
				{
					elements.add(exclude ? AllEntitiesElement.EXCLUDE_ALL : AllEntitiesElement.INCLUDE_ALL);
					continue;
				}

				if (name.endsWith(":*"))
				{
					if (name.length() == 2) { throw new FilterParserException(); }
					String namespace = name.substring(0, name.length() - 2);
					if (!ResourceLocation.isValidNamespace(namespace)) { throw new FilterParserException(); }

					elements.add(new AllEntitiesElement(exclude, namespace));
					continue;
				}

				ResourceLocation resLoc = parseToResLoc(name);
				Element lastElement = elements.isEmpty() ? null : elements.get(elements.size() - 1);

				boolean reuseEntitySet = (lastElement instanceof EntitySetElement && lastElement.exclude == exclude);
				EntitySetElement entitySetElement = reuseEntitySet ? (EntitySetElement)lastElement : new EntitySetElement(exclude);
				entitySetElement.add(resLoc);
				if (!reuseEntitySet) { elements.add(entitySetElement); }
			}
		}
	}

	public static @Nullable EntityFilterInstance create(String str)
	{
		try { return new EntityFilterInstance(str); }
		catch (FilterParserException e) { return null; }
	}

	public static boolean test(String str)
	{
		try
		{
			new EntityFilterInstance(str);
			return true;
		}
		catch (FilterParserException e) { return false; }
	}

	public boolean isAllowed(Entity entity)
	{
		boolean allowed = false;
		for (Element element : elements)
		{
			switch (element.isAllowed(entity))
			{
				case ALLOW -> allowed = true;
				case DENY -> allowed = false;
				case IGNORE -> {}
			}
		}
		return allowed;
	}

	public boolean isEmpty()
	{
		return elements.isEmpty();
	}

	private static ResourceLocation parseToResLoc(String str) throws FilterParserException
	{
		ResourceLocation resLoc = ResourceLocation.tryParse(str);
		if (resLoc == null) { throw new FilterParserException(); }
		return resLoc;
	}

	private static abstract class Element
	{
		private final boolean exclude;

		protected Element(boolean exclude)
		{
			this.exclude = exclude;
		}

		public FilterElementResults isAllowed(Entity entity)
		{
			return applies(entity)
					? (exclude ? FilterElementResults.DENY : FilterElementResults.ALLOW)
					: FilterElementResults.IGNORE;
		}

		protected abstract boolean applies(Entity entity);
	}

	private static class GroupElement extends Element
	{
		public final String name;
		private final List<Class<?>> parents;

		public GroupElement(boolean exclude, String name, List<Class<?>> parents)
		{
			super(exclude);
			this.name = name;
			this.parents = parents;
		}

		protected boolean applies(Entity entity)
		{
			for (Class<?> parent : parents)
			{
				if (parent.isInstance(entity)) { return true; }
			}
			return false;
		}
	}

	private static class TagElement extends Element
	{
		public final String tag;

		public TagElement(boolean exclude, String tag)
		{
			super(exclude);
			this.tag = tag;
		}

		@Override protected boolean applies(Entity entity)
		{
			return entity.getTags().contains(tag);
		}
	}

	private static class EntitySetElement extends Element
	{
		public final Set<EntityType<?>> set = new HashSet<>();

		public EntitySetElement(boolean exclude)
		{
			super(exclude);
		}

		public void add(ResourceLocation resLoc)
		{
			Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(resLoc);
			entityType.ifPresent(set::add);
		}

		@Override protected boolean applies(Entity entity)
		{
			return set.contains(entity.getType());
		}
	}

	private static class AllEntitiesElement extends Element
	{
		public final static AllEntitiesElement INCLUDE_ALL = new AllEntitiesElement(false, null);
		public final static AllEntitiesElement EXCLUDE_ALL = new AllEntitiesElement(false, null);
		private final @Nullable String fromNamespace;

		public AllEntitiesElement(boolean exclude, @Nullable String fromNamespace)
		{
			super(exclude);
			this.fromNamespace = fromNamespace;
		}

		@Override protected boolean applies(Entity entity)
		{
			if (fromNamespace == null) { return true; }
			return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals(fromNamespace);
		}
	}

	public static class FilterParserException extends Exception {}

	private enum FilterElementResults
	{
		ALLOW, DENY, IGNORE
	}

	private enum Group
	{
		VEHICLES(List.of(Saddleable.class, Minecart.class, Boat.class)),
		PROJECTILES(List.of(Projectile.class)),
		ITEMS(List.of(ItemEntity.class)),
		MOBS(List.of(Mob.class)),
		MINECARTS(List.of(AbstractMinecart.class));

		public final String name;
		public final GroupElement include, exclude;

		Group(List<Class<?>> parent)
		{
			this.name = name().toLowerCase();
			this.include = new GroupElement(false, name, parent);
			this.exclude = new GroupElement(true, name, parent);
		}

		public static @Nullable GroupElement fromString(boolean exclude, String str)
		{
			try
			{
				Group group = valueOf(str.toUpperCase());
				return exclude ? group.exclude : group.include;
			}
			catch (IllegalArgumentException e) { return null; }
		}
	}
}
