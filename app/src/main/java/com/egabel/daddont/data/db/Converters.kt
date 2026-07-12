package com.egabel.daddont.data.db

import androidx.room.TypeConverter
import com.egabel.daddont.data.model.Category
import com.egabel.daddont.data.model.ImpulseKind
import com.egabel.daddont.data.model.Prediction
import com.egabel.daddont.data.model.Tier
import com.egabel.daddont.data.model.Verdict
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
    fun fromVerdict(value: Verdict?): String? = value?.name

    @TypeConverter
    fun toVerdict(value: String?): Verdict? = value?.let { Verdict.valueOf(it) }

    @TypeConverter
    fun fromPrediction(value: Prediction?): String? = value?.name

    @TypeConverter
    fun toPrediction(value: String?): Prediction? = value?.let { Prediction.valueOf(it) }

    @TypeConverter
    fun fromKind(value: ImpulseKind): String = value.name

    @TypeConverter
    fun toKind(value: String): ImpulseKind = ImpulseKind.valueOf(value)
}
