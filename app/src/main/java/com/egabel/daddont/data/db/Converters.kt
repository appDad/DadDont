package com.egabel.daddont.data.db

import androidx.room.TypeConverter
import com.egabel.daddont.data.model.Category
import com.egabel.daddont.data.model.DismissalType
import com.egabel.daddont.data.model.Tier
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromUUID(value: UUID): String = value.toString()

    @TypeConverter
    fun toUUID(value: String): UUID = UUID.fromString(value)

    @TypeConverter
    fun fromTier(value: Tier?): String? = value?.name

    @TypeConverter
    fun toTier(value: String?): Tier? = value?.let { Tier.valueOf(it) }

    @TypeConverter
    fun fromCategory(value: Category?): String? = value?.name

    @TypeConverter
    fun toCategory(value: String?): Category? = value?.let { Category.valueOf(it) }

    @TypeConverter
    fun fromDismissalType(value: DismissalType?): String? = value?.name

    @TypeConverter
    fun toDismissalType(value: String?): DismissalType? = value?.let { DismissalType.valueOf(it) }
}
