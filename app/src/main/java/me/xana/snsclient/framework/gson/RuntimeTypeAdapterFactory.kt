package me.xana.snsclient.framework.gson

import com.google.gson.*
import com.google.gson.internal.Streams
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import me.xana.snsclient.R
import java.io.IOException
import java.util.LinkedHashMap

/**
 * Adapts values whose runtime type may differ from their declaration type. This
 * is necessary when a field's type is not the same type that GSON should create
 * when deserializing that field. For example, consider these types:
 * <pre>   {@code
 *   abstract class Shape {
 *     int x;
 *     int y;
 *   }
 *   class Circle extends Shape {
 *     int radius;
 *   }
 *   class Rectangle extends Shape {
 *     int width;
 *     int height;
 *   }
 *   class Diamond extends Shape {
 *     int width;
 *     int height;
 *   }
 *   class Drawing {
 *     Shape bottomShape;
 *     Shape topShape;
 *   }
 * }</pre>
 * <p>Without additional type information, the serialized JSON is ambiguous. Is
 * the bottom shape in this drawing a rectangle or a diamond? <pre>   {@code
 *   {
 *     "bottomShape": {
 *       "width": 10,
 *       "height": 5,
 *       "x": 0,
 *       "y": 0
 *     },
 *     "topShape": {
 *       "radius": 2,
 *       "x": 4,
 *       "y": 1
 *     }
 *   }}</pre>
 * This class addresses this problem by adding type information to the
 * serialized JSON and honoring that type information when the JSON is
 * deserialized: <pre>   {@code
 *   {
 *     "bottomShape": {
 *       "type": "Diamond",
 *       "width": 10,
 *       "height": 5,
 *       "x": 0,
 *       "y": 0
 *     },
 *     "topShape": {
 *       "type": "Circle",
 *       "radius": 2,
 *       "x": 4,
 *       "y": 1
 *     }
 *   }}</pre>
 * Both the type field name ({@code "type"}) and the type labels ({@code
 * "Rectangle"}) are configurable.
 *
 * <h3>Registering Types</h3>
 * Create a {@code RuntimeTypeAdapterFactory} by passing the base type and type field
 * name to the {@link #of} factory method. If you don't supply an explicit type
 * field name, {@code "type"} will be used. <pre>   {@code
 *   RuntimeTypeAdapterFactory<Shape> shapeAdapterFactory
 *       = RuntimeTypeAdapterFactory.of(Shape.class, "type");
 * }</pre>
 * Next register all of your subtypes. Every subtype must be explicitly
 * registered. This protects your application from injection attacks. If you
 * don't supply an explicit type label, the type's simple name will be used.
 * <pre>   {@code
 *   shapeAdapterFactory.registerSubtype(Rectangle.class, "Rectangle");
 *   shapeAdapterFactory.registerSubtype(Circle.class, "Circle");
 *   shapeAdapterFactory.registerSubtype(Diamond.class, "Diamond");
 * }</pre>
 * Finally, register the type adapter factory in your application's GSON builder:
 * <pre>   {@code
 *   Gson gson = new GsonBuilder()
 *       .registerTypeAdapterFactory(shapeAdapterFactory)
 *       .create();
 * }</pre>
 * Like {@code GsonBuilder}, this API supports chaining: <pre>   {@code
 *   RuntimeTypeAdapterFactory<Shape> shapeAdapterFactory = RuntimeTypeAdapterFactory.of(Shape.class)
 *       .registerSubtype(Rectangle.class)
 *       .registerSubtype(Circle.class)
 *       .registerSubtype(Diamond.class);
 * }</pre>
 *
 */
class RuntimeTypeAdapterFactory<T : Any> private constructor(
    private val baseType: Class<T>,
    private val typeFieldName: String,
    private val maintainType: Boolean
): TypeAdapterFactory {

    private val labelToSubtype = LinkedHashMap<String, Class<out T>>()
    private val subtypeToLabel = LinkedHashMap<Class<out T>, String>()

    override fun <R : Any> create(gson: Gson, type: TypeToken<R>?): TypeAdapter<R>? {
        if (type?.rawType != baseType) {
            return null
        }

        val labelToDelegate = LinkedHashMap<String, TypeAdapter<*>>()
        val subtypeToDelegate = LinkedHashMap<Class<*>, TypeAdapter<*>>()
        for ((key, value) in labelToSubtype) {
            val delegate = gson.getDelegateAdapter(this, TypeToken.get(value)) ?: continue
            labelToDelegate[key] = delegate
            subtypeToDelegate[value] = delegate
        }

        return object : TypeAdapter<R>() {
            @Throws(IOException::class)
            override fun read(reader: JsonReader): R {
                val jsonElement = Streams.parse(reader)
                val labelJsonElement: JsonElement?
                labelJsonElement = if (maintainType) {
                    jsonElement.asJsonObject.get(typeFieldName)
                } else {
                    jsonElement.asJsonObject.remove(typeFieldName)
                }

                if (labelJsonElement == null) {
                    throw JsonParseException(
                        "cannot deserialize " + baseType
                                + " because it does not define a field named " + typeFieldName
                    )
                }
                val label = labelJsonElement.asString
                val delegate = labelToDelegate[label] as? TypeAdapter<R> ?: throw JsonParseException(
                    "cannot deserialize " + baseType + " subtype named "
                            + label + "; did you forget to register a subtype?"
                )// registration requires that subtype extends T
                return delegate.fromJsonTree(jsonElement)
            }

            @Throws(IOException::class)
            override fun write(out: JsonWriter, value: R) {
                val srcType = value.javaClass
                val label = subtypeToLabel[srcType as Class<out T>]
                val delegate = subtypeToDelegate[srcType] as? TypeAdapter<R> ?: throw JsonParseException(
                    "cannot serialize " + srcType.getName()
                            + "; did you forget to register a subtype?"
                )// registration requires that subtype extends T
                val jsonObject = delegate.toJsonTree(value).asJsonObject

                if (maintainType) {
                    Streams.write(jsonObject, out)
                    return
                }

                val clone = JsonObject()

                if (jsonObject.has(typeFieldName)) {
                    throw JsonParseException(
                        "cannot serialize " + srcType.getName()
                                + " because it already defines a field named " + typeFieldName
                    )
                }
                clone.add(typeFieldName, JsonPrimitive(label!!))

                for (e in jsonObject.entrySet()) {
                    clone.add(e.key, e.value)
                }
                Streams.write(clone, out)
            }
        }.nullSafe()
    }

     /**
     * Registers `type` identified by `label`. Labels are case
     * sensitive.
     *
     * @throws IllegalArgumentException if either `type` or `label`
     * have already been registered on this type adapter.
     */
    fun registerSubtype(type: Class<out T>, label: String): RuntimeTypeAdapterFactory<T> {
        if (subtypeToLabel.containsKey(type) || labelToSubtype.containsKey(label)) {
            throw IllegalArgumentException("types and labels must be unique")
        }
        labelToSubtype[label] = type
        subtypeToLabel[type] = label
        return this
    }

    /**
     * Registers `type` identified by its [simple][Class.getSimpleName]. Labels are case sensitive.
     *
     * @throws IllegalArgumentException if either `type` or its simple name
     * have already been registered on this type adapter.
     */
    fun registerSubtype(type: Class<out T>): RuntimeTypeAdapterFactory<T> {
        return registerSubtype(type, type.simpleName)
    }

    companion object {
        /**
         * Creates a new runtime type adapter using for `baseType` using `typeFieldName` as the type field name. Type field names are case sensitive.
         * `maintainType` flag decide if the type will be stored in pojo or not.
         */
        fun <T : Any> of(baseType: Class<T>, typeFieldName: String, maintainType: Boolean): RuntimeTypeAdapterFactory<T> {
            return RuntimeTypeAdapterFactory(baseType, typeFieldName, maintainType)
        }

        /**
         * Creates a new runtime type adapter using for `baseType` using `typeFieldName` as the type field name. Type field names are case sensitive.
         */
        fun <T : Any> of(baseType: Class<T>, typeFieldName: String): RuntimeTypeAdapterFactory<T> {
            return RuntimeTypeAdapterFactory(baseType, typeFieldName, false)
        }

        /**
         * Creates a new runtime type adapter for `baseType` using `"type"` as
         * the type field name.
         */
        fun <T : Any> of(baseType: Class<T>): RuntimeTypeAdapterFactory<T> {
            return RuntimeTypeAdapterFactory(baseType, "type", false)
        }
    }
}