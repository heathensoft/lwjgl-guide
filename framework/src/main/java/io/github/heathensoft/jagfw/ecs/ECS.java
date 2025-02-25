package io.github.heathensoft.jagfw.ecs;

import io.github.heathensoft.jagfw.core.Disposable;
import io.github.heathensoft.jagfw.core.Resolution;

import java.util.*;

import static java.lang.Long.bitCount;

/**
 * Frederik Dahl 2/22/2025
 */
public class ECS implements Disposable {

    public static final int MIN_COMPONENT_TYPES = 16;
    public static final int MAX_COMPONENT_TYPES = 256;
    public static final int MIN_ENTITY_CAPACITY = 128;
    private final Map<Class<? extends ECSystem>,ECSystem> system_class_to_system;
    private final Map<Class<?>,Integer> component_class_to_type;
    private final List<ECSystem> system_pipeline;
    private final List<BitSet> system_access_masks;
    private final List<BitSet> system_denied_masks;
    private final List<BitSet> entity_component_masks;
    private final List<EntityArray> system_entities;
    private final Object[][] entity_components;
    private final EntityPool entity_handles;
    private final Object shared_context;
    private boolean dirty_flag;
    private boolean processing;
    private boolean signaled_to_exit;

    public ECS(Object context, int entity_capacity, int component_type_capacity) {
        component_type_capacity = Math.max(MIN_COMPONENT_TYPES,
        Math.min(MAX_COMPONENT_TYPES,component_type_capacity));
        entity_capacity = Math.max(MIN_ENTITY_CAPACITY,entity_capacity);
        system_class_to_system = new HashMap<>(128);
        component_class_to_type = new HashMap<>(128);
        system_pipeline = new ArrayList<>(64);
        system_access_masks = new ArrayList<>(64);
        system_denied_masks = new ArrayList<>(64);
        system_entities = new ArrayList<>(64);
        entity_component_masks = new ArrayList<>(entity_capacity);
        entity_components = new Object[entity_capacity][component_type_capacity];
        entity_handles = new EntityPool(entity_capacity);
        for (int i = 0; i < entity_capacity; i++) {
            entity_component_masks.add(new BitSet(component_type_capacity));
        } shared_context = context;
    }


    public void signalToExit() {
        signaled_to_exit = true;
    }

    public boolean shouldExit() {
        return signaled_to_exit;
    }


    public void dispose() {
        if (processing) throw new IllegalStateException(
        "dispose cannot be called from within the ecs");
        processing = true;
        for (ECSystem system : system_pipeline) {
            system.dispose();
        } system_pipeline.clear();
        // todo: add more here
        processing = false;
    }

    public void update(float dt) { refresh();
        if (!processing) { processing = true;
            int num_systems = system_pipeline.size();
            for (int s = 0; s < num_systems; s++) {
                ECSystem next = system_pipeline.get(s);
                if (shouldExit()) {
                    processing = false;
                    return;
                } if (!next.isPaused()) {
                    if (next instanceof ProcessSystem system) {
                        system.preProcessing(this,dt);
                        EntityArray entities = system_entities.get(s);
                        for (int entity = 0; entity < entities.size; entity++) {
                            system.process(this,entity,dt);
                        } system.postProcessing(this,dt);
                    } else next.processSystem(this,dt);
                } refresh();
            } processing = false;
        }
    }

    public void render(float alpha) { refresh();
        if (!processing) { processing = true;
            int num_systems = system_pipeline.size();
            for (int s = 0; s < num_systems; s++) {
                ECSystem next = system_pipeline.get(s);
                if (shouldExit()) {
                    processing = false;
                    return;
                } if (!next.isPaused()) {
                    if (next instanceof RenderSystem system) {
                        system.preRender(this,alpha);
                        EntityArray entities = system_entities.get(s);
                        for (int entity = 0; entity < entities.size; entity++) {
                            system.render(this,entity,alpha);
                        } system.postRender(this,alpha);
                    } else next.renderSystem(this,alpha);
                } refresh();
            } processing = false;
        }
    }

    public void resizeEvent(Resolution resolution) {
        if (processing) throw new IllegalStateException(
                "dispose cannot be called from within the ecs");
        processing = true;
        for (ECSystem system : system_pipeline) {
            system.resizeEvent(this,resolution);
        } processing = false;
    }

    public void addSystemToPipeline(ECSystem system) {
        Class<? extends ECSystem> system_class = system.getClass();
        ECSystem existing = system_class_to_system.putIfAbsent(system_class,system);
        if(existing == null) { dirty_flag = true;
            List<Class<?>> required_components = new LinkedList<>();
            List<Class<?>> blocking_components = new LinkedList<>();
            BitSet access_mask = new BitSet(componentTypeLimit());
            BitSet denied_mask = new BitSet(componentTypeLimit());
            system.defineAccess(required_components,blocking_components);
            for (Class<?> clazz : required_components) {
                access_mask.set(componentType(clazz));
            } for (Class<?> clazz : blocking_components) {
                denied_mask.set(componentType(clazz));
            } system_access_masks.addLast(access_mask);
            system_denied_masks.addLast(denied_mask);
            system_pipeline.addLast(system);
            if (system instanceof ProcessSystem || system instanceof RenderSystem) {
                system_entities.addLast(new EntityArray(MIN_ENTITY_CAPACITY,entityCapacity()));
            } else system_entities.addLast(new EntityArray(0,0));
        } else throw new RuntimeException("cannot have multiple systems of same class");
    }

    public int newEntity() { // -1 if cap reached
        return entity_handles.obtain();
    }

    public void deleteEntity(int entity) {
        if (entity_handles.validate(entity)) {
            int num_types = numComponentTypes();
            BitSet entity_mask = entity_component_masks.get(entity);
            for (int i = 0; i < num_types; i++) {
                if (entity_mask.get(i)) entity_components[entity][i] = null;
            } entity_mask.clear();
            entity_handles.free(entity);
            dirty_flag = true;
        }
    }

    public void addComponent(int entity, Object component, boolean replace) {
        if (component != null && entity_handles.validate(entity)) {
            int type = componentType(component.getClass());
            BitSet entity_mask = entity_component_masks.get(entity);
            if (entity_mask.get(type)) {
                if (replace) entity_components[entity][type] = component;
            } else { entity_mask.set(type);
                entity_components[entity][type] = component;
                dirty_flag = true;
            }
        }
    }

    public Object removeComponent(int entity, Class<?> component_class) {
        if (component_class != null && entity_handles.validate(entity)) {
            int type = componentType(component_class);
            BitSet entity_mask = entity_component_masks.get(entity);
            if (entity_mask.get(type)) {
                entity_mask.unSet(type);
                Object component = entity_components[entity][type];
                entity_components[entity][type] = null;
                if (entity_mask.cardinality() == 0) {
                    entity_handles.free(entity);
                } dirty_flag = true;
                return component; }
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

    private void refresh() {
        // rebuilds system entity arrays
        if (dirty_flag) { dirty_flag = false;
            int num_systems = system_pipeline.size();
            for (int s = 0; s < num_systems; s++) {
                ECSystem system = system_pipeline.get(s);
                if (system instanceof ProcessSystem || system instanceof RenderSystem) {
                    EntityArray entity_array = system_entities.get(s);
                    entity_array.clear();
                    final boolean[] all_entities = entity_handles.array();
                    final int peak = entity_handles.peak();
                    for (int i = 0; i <= peak; i++) {
                        if (all_entities[i]) {
                            if (entityMemberOf(i,s)) {
                                entity_array.add(i);
                            }
                        }
                    }
                }
            }
        }
    }

    private int componentType(Class<?> clazz) {
        Integer component_id = component_class_to_type.get(clazz);
        if (component_id == null) { dirty_flag = true;
            component_id = numComponentTypes();
            if (component_id == componentTypeLimit()) {
                throw new RuntimeException("Component type limit reached (" + componentTypeLimit() + ")");
            } component_class_to_type.put(clazz,component_id);
        } return component_id;
    }

    private boolean entityMemberOf(int entity, int system) {
        BitSet entity_mask = entity_component_masks.get(entity);
        BitSet system_access = system_access_masks.get(system);
        BitSet system_denied = system_denied_masks.get(system);
        return checkRequirements(entity_mask,system_access,system_denied);
    }



    private static boolean checkRequirements(BitSet entity, BitSet system_access, BitSet system_denied) {
        int words = system_access.mask.length;
        for (int i = 0; i < words; i++) {
            final long ew = entity.mask[i];
            final long sa = system_access.mask[i];
            final long sd = system_denied.mask[i];
            if (((ew & sa) != sa) || (ew & sd) != 0) return false;
        } return true;
    }



    private static final class BitSet {
        final long[] mask;
        BitSet(int bits) {
            int words = (int) org.joml.Math.ceil(bits / 64f);
            mask = new long[words];
        } boolean get(int index) {
            final int word = index >>> 6;
            return (mask[word] & (1L << index)) != 0L;
        } void set(int index) {
            int word = index >>> 6;
            mask[word] |= 1L << index;
        } void unSet(int index) {
            int word = index >>> 6;
            mask[word] &= ~(1L << index);
        } void clear() {
            Arrays.fill(mask,0L);
        } int cardinality() {
            int count = 0;
            for (long word : mask) {
                if (word == 0L) continue;
                count += bitCount(word);
            } return count;
        }
    }

    private static final class EntityArray {
        private final int cap;
        private int size;
        private int[] array;
        EntityArray(int len, int max_len) {
            if (len < 0) throw new NegativeArraySizeException("argument length < 0");
            array = new int[len];
            cap = Math.max(len,max_len);
        } void add(int entity) {
            if (size == array.length) {
                if (size == cap) {
                    throw new RuntimeException("array capacity at maximum");
                } if (array.length == 0) {
                    array = new int[1];
                } else { int[] tmp = array;
                    array = new int[Math.min(size * 2,cap)];
                    System.arraycopy(tmp,0,array,0,size);}
            } array[size++] = entity;
        } boolean remove(int entity) {
            for (int i = 0; i < size; i++) {
                if (array[i] == entity) {
                    array[i] = array[--size];
                    return true; }
            } return false;
        } void clear() {
            size = 0;
        }
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
        } int peak() { return peak; }
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
        } void free(int e) {
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
        } int obtain() {
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
}
