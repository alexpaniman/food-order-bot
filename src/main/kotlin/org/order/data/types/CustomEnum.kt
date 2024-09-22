import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.postgresql.util.PGobject


inline fun <reified T : Enum<T>> getEnumName() = "${T::class.simpleName!!.lowercase()}_enum"

inline fun <reified T : Enum<T>> Transaction.createOrUpdatePostgreSQLEnum(enumValues: Array<T>) {
    val valueNames = enumValues.map { it.name }
    val psqlType = getEnumName<T>()
    val joined = valueNames.joinToString { "'$it'" }

    val alreadyInsertedEnumValues = mutableSetOf<String>()

    exec("SELECT n.nspname AS enum_schema, t.typname AS enum_name, e.enumlabel AS enum_value " +
            "FROM pg_type t JOIN pg_enum e ON t.oid = e.enumtypid JOIN pg_catalog.pg_namespace n " +
            "ON n.oid = t.typnamespace WHERE t.typname = '$psqlType'") {
        while (it.next())
            alreadyInsertedEnumValues.add(it.getString("enum_value"))
    }

    val missingEnums = valueNames.filter { it !in alreadyInsertedEnumValues }

    if (alreadyInsertedEnumValues.isEmpty())
        exec("CREATE TYPE $psqlType AS ENUM ($joined);")
    else if (missingEnums.isNotEmpty())
        for (missingEnum in missingEnums)
            exec("ALTER TYPE $psqlType ADD VALUE '$missingEnum';")
}

class PGEnum(enumTypeName: String, enumValue: String) : PGobject() {
    init {
        value = enumValue
        type = enumTypeName
    }

    override fun toString() = value.toString()
}

inline fun <reified T : Enum<T>> Table.postgresEnumeration(
    columnName: String
) = registerColumn<T>(columnName, object : ColumnType() {
        override fun sqlType(): String = getEnumName<T>()
        override fun valueFromDB(value: Any) = enumValueOf<T>(value.toString())
        override fun notNullValueToDB(value: Any) = PGEnum(sqlType(), value.toString())
    })
