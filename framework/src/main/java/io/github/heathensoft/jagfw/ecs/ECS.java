package io.github.heathensoft.jagfw.ecs;

import org.joml.Math;

import java.util.*;

import static java.lang.Long.bitCount;
import static java.lang.Long.numberOfTrailingZeros;

/**
 * Frederik Dahl 2/19/2025
 */
public class ECS {

    public static final int MIN_COMPONENT_TYPES = 16;
    public static final int MAX_COMPONENT_TYPES = 256;
    public static final int MIN_ENTITY_CAPACITY = 128;
    private final Map<Class<? extends ECSystem>,ECSystem> system_class_to_system;
    private final Map<Class<?>,Integer> component_class_to_type;
    private final List<ECSystem> system_pipeline;
    private final List<ComponentMask> system_access_masks;
    private final List<ComponentMask> system_denied_masks;
    private final List<ComponentMask> entity_component_masks;
    private final Object[][] entity_components;
    private final EntityPool entity_handles;
    private Object shared_context;

    public ECS(int entity_capacity, int component_type_capacity) {
        component_type_capacity = Math.max(MIN_COMPONENT_TYPES,Math.min(MAX_COMPONENT_TYPES,component_type_capacity));
        entity_capacity = Math.max(MIN_ENTITY_CAPACITY,entity_capacity);
        system_class_to_system = new HashMap<>(64);
        component_class_to_type = new HashMap<>(64);
        system_pipeline = new ArrayList<>(64);
        system_access_masks = new ArrayList<>(64);
        system_denied_masks = new ArrayList<>(64);
        entity_component_masks = new ArrayList<>(entity_capacity);
        entity_components = new Object[entity_capacity][component_type_capacity];
        entity_handles = new EntityPool(entity_capacity);
        for (int i = 0; i < entity_capacity; i++) {
            entity_component_masks.add(new ComponentMask(component_type_capacity));
        }
    }

    public void update(float dt) {
        int num_systems = system_pipeline.size();
        for (int s = 0; s < num_systems; s++) {
            ECSystem next = system_pipeline.get(s);
            if (!next.isPaused()) {
                if (next instanceof ProcessSystem system) {
                    system.preProcessing(this,dt);
                    boolean[] entity_array = entity_handles.array();
                    final int peak = entity_handles.peak();
                    for (int i = 0; i < peak; i++) {
                        if (entity_array[i]) {
                            if (entityMemberOf(i,s))
                                system.process(this,i,dt);}
                    } system.postProcessing(this,dt);
                } else next.processSystem(this,dt);
            }
        }
    }

    public void render(float alpha) {
        int num_systems = system_pipeline.size();
        for (int s = 0; s < num_systems; s++) {
            ECSystem next = system_pipeline.get(s);
            if (!next.isPaused()) {
                if (next instanceof RenderSystem system) {
                    system.preRender(this,alpha);
                    boolean[] entity_array = entity_handles.array();
                    final int peak = entity_handles.peak();
                    for (int i = 0; i < peak; i++) {
                        if (entity_array[i]) {
                            if (entityMemberOf(i,s))
                                system.render(this,i,alpha); }
                    } system.postRender(this,alpha);
                } else next.renderSystem(this,alpha);
            }
        }
    }

    public void addSystemToPipeline(ECSystem system) {
        Class<? extends ECSystem> system_class = system.getClass();
        ECSystem existing = system_class_to_system.putIfAbsent(system_class,system);
        if(existing == null) { List<Class<?>> required_components = new LinkedList<>();
            List<Class<?>> blocking_components = new LinkedList<>();
            ComponentMask access_mask = new ComponentMask(componentTypeLimit());
            ComponentMask denied_mask = new ComponentMask(componentTypeLimit());
            system.defineAccess(required_components,blocking_components);
            for (Class<?> clazz : required_components) {
                access_mask.set(componentType(clazz));
            } for (Class<?> clazz : blocking_components) {
                denied_mask.set(componentType(clazz));
            } system_access_masks.addLast(access_mask);
            system_denied_masks.addLast(denied_mask);
            system_pipeline.addLast(system);
        } else throw new RuntimeException("cannot have multiple systems of same class");
    }

    public int createEntity() {
        return entity_handles.obtain(); // -1 if cap reached
    }

    public void deleteEntity(int entity) {
        if (entity_handles.validate(entity)) {
            int num_types = numComponentTypes();
            ComponentMask entity_mask = entity_component_masks.get(entity);
            for (int i = 0; i < num_types; i++) {
                if (entity_mask.get(i)) entity_components[entity][i] = null;
            } entity_mask.clear();
            entity_handles.free(entity);
        }
    }

    public void addComponent(int entity, Object component, boolean replace) {
        if (component != null && entity_handles.validate(entity)) {
            int type = componentType(component.getClass());
            ComponentMask entity_mask = entity_component_masks.get(entity);
            if (!entity_mask.get(type) || replace) {
                entity_mask.set(type);
                entity_components[entity][type] = component;
            }
        }
    }

    public Object removeComponent(int entity, Class<?> component_class) {
        if (component_class != null && entity_handles.validate(entity)) {
            int type = componentType(component_class);
            ComponentMask entity_mask = entity_component_masks.get(entity);
            if (entity_mask.get(type)) {
                entity_mask.unSet(type);
                Object component = entity_components[entity][type];
                entity_components[entity][type] = null;
                if (entity_mask.cardinality() == 0) {
                    entity_handles.free(entity);
                } return component;
            }
        } return null;
    }

    public <T> T getComponent(int entity, Class<T> component_class) {
        if (entity_handles.validate(entity)) {
            int type = componentType(component_class);
            return component_class.cast(entity_components[entity][type]);
        } return null;
    }

    public <T extends ECSystem> T getSystem(Class<T> system_class) {
        return system_class.cast(system_class_to_system.get(system_class));
    }

    public <T> T getSharedContext(Class<T> clazz) {
        if (shared_context != null) {
            Class<?> context_class = shared_context.getClass();
            if (clazz.isAssignableFrom(context_class))
                return clazz.cast(shared_context);
        } return null;
    }

    public Object setSharedContext(Object context) {
        Object existing = shared_context;
        shared_context = context;
        return existing;
    }

    public int numComponentTypes() {
        return component_class_to_type.size();
    }

    public int numEntities() {
        return entity_handles.count();
    }

    public int componentTypeLimit() {
        return entity_components[0].length;
    }

    public int entityCapacity() {
        return entity_handles.capacity();
    }

    private int componentType(Class<?> clazz) {
        Integer component_id = component_class_to_type.get(clazz);
        if (component_id == null) {
            component_id = numComponentTypes();
            if (component_id == componentTypeLimit()) {
                throw new RuntimeException("Component type limit reached (" + componentTypeLimit() + ")");
            } component_class_to_type.put(clazz,component_id);
        } return component_id;
    }

    private boolean entityMemberOf(int entity, int system) {
        ComponentMask entity_mask = entity_component_masks.get(entity);
        ComponentMask system_access = system_access_masks.get(system);
        ComponentMask system_denied = system_denied_masks.get(system);
        return ComponentMask.checkRequirements(entity_mask,system_access,system_denied);
    }

    private static final class EntityPool {

        private int peak;
        private int gaps;
        private int count;
        private final int cap;
        private final boolean[] slots;

        EntityPool(int capacity) {
            peak = -1;
            cap = capacity;
            slots = new boolean[cap];
        }

        int peak() { return peak; }
        int available() { return cap - count; }
        int count() { return count; }
        int capacity() { return cap; }
        boolean[] array() { return slots; }
        boolean validate(int e) {
            return e >= 0 && e <= peak && slots[e];
        } void clear() {
            Arrays.fill(slots,false);
            peak = -1;
            count = 0;
            gaps = 0;
        }

        void free(int e) {
            if (validate(e)) {
                slots[e] = false;
                count--;
                gaps++;
                if (e == peak) {
                    for (int i = e; i >= 0; i--) {
                        if (slots[i]) {
                            break;
                        } else {
                            gaps--;
                            peak--;
                        }
                    }
                }
            }
        }

        int obtain() {
            if (count < cap) {
                if (gaps == 0) {
                    slots[count] = true;
                    peak = count;
                    return count++;
                } for (int e = 0; e < count; e++) {
                    if (!slots[e]) {
                        slots[e] = true;
                        gaps--;
                        count++;
                        return e;
                    }
                }
            } return -1;
        }
    }

    private static final class ComponentMask {

        private final long[] mask;

        ComponentMask(int bits) {
            int words = (int) Math.ceil(bits / 64f);
            mask = new long[words];
        }

        boolean get(int index) {
            final int word = index >>> 6;
            return (mask[word] & (1L << index)) != 0L;
        }

        void set(int index) {
            int word = index >>> 6;
            mask[word] |= 1L << index;
        }

        void unSet(int index) {
            int word = index >>> 6;
            mask[word] &= ~(1L << index);
        }

        void clear() {
            Arrays.fill(mask,0L);
        }

        int cardinality() {
            int count = 0;
            for (long word : mask) {
                if (word == 0L) continue;
                count += bitCount(word);
            } return count;
        }

        public String toString() {
            int count = 0;
            int cardinality = cardinality();
            int end = Math.min(128, cardinality);
            StringBuilder sb = new StringBuilder();
            sb.append("Mask[").append(cardinality);
            if (cardinality > 0) {
                sb.append(": {");
                for (int i = nextSetBit(0);
                     end > count && i != -1;
                     i = nextSetBit(i + 1)) {
                    if (count != 0) sb.append(", ");
                    sb.append(i); count++;
                } if (cardinality > end)
                    sb.append(" ...");
                sb.append("}");
            } sb.append("]");
            return sb.toString();
        }

        private int nextSetBit(int from) {
            final int word = from >>> 6;
            if (word < mask.length) {
                long map = mask[word] >>> from;
                if (map == 0) {
                    for (int i = 1 + word; i < mask.length; i++)
                        if (mask[i] != 0) return i * 64 + numberOfTrailingZeros(mask[i]);
                } else return from + numberOfTrailingZeros(map);
            } return -1;
        }

        static boolean checkRequirements(ComponentMask entity, ComponentMask system_access, ComponentMask system_denied) {
            int words = system_access.mask.length;
            for (int i = 0; i < words; i++) {
                final long ew = entity.mask[i];
                final long sa = system_access.mask[i];
                final long sd = system_denied.mask[i];
                if (((ew & sa) != sa) || (ew & sd) != 0) return false;
            } return true;
        }
    }
}
