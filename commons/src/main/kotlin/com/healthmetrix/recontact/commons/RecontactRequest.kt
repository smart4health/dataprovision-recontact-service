package com.healthmetrix.recontact.commons

const val INDENT = "\n            "
data class RecontactRequest(
    val cohort: Cohort,
) {
    data class Cohort(
        val name: String,
        val distribution: Distribution,
        val datasetVersion: String,
        val pseudonymIds: List<String>,
        val queryParams: List<QueryParam>,
    ) {
        data class Distribution(
            val age: List<AgeDistributionItem>,
            val gender: List<GenderDistributionItem>,
        )

        data class AgeDistributionItem(
            val min: Int? = null,
            val max: Int? = null,
            val count: Int,
        ) {
            override fun toString(): String = "[${min ?: "N/A"} - ${max ?: "N/A"}]: $count"
        }

        data class GenderDistributionItem(
            val gender: String,
            val count: Int,
        ) {
            override fun toString(): String = "${gender.lowercase().replaceFirstChar { it.uppercase() }}: $count"
        }

        data class QueryParam(
            val content: List<Content>,
        ) {
            override fun toString(): String =
                """
                ${content.joinToString(separator = "; ")}
                """.trimIndent()

            data class Content(
                val name: String,
                val attributes: List<Attribute>,
                val isExcluded: Boolean,
            ) {
                override fun toString(): String =
                    """
                    + Filter Name: "$name" | Excluded: ${if (isExcluded) "yes" else "no"}${
                        if (attributes.isEmpty()) {
                            ""
                        } else {
                            " | Attributes: ${
                                attributes.joinToString(",")
                            }"
                        }
                    }
                    """.trimIndent()

                data class Attribute(
                    val name: String,
                    val constraints: List<String>,
                ) {
                    override fun toString(): String = "[$name: ${constraints.joinToString(", ")}]"
                }
            }
        }

        override fun toString(): String =
            """
            Size: ${this.pseudonymIds.toSet().size}
            Dataset version: ${this.datasetVersion}
            
            Age distribution: 
            ${
                this.distribution.age.sortedBy { it.min }.joinToString(separator = INDENT) { "+ $it" }
            }
            
            Gender distribution: 
            ${
                this.distribution.gender.sortedBy { it.gender }.joinToString(separator = INDENT) { "+ $it" }
            }
            
            Query parameters:
            ${if (queryParams.isEmpty()) "none" else queryParams.joinToString(separator = INDENT)}
            """.trimIndent()
    }

    fun toCohortInfoMessage(): String = cohort.toString()
}
