/*
 Copyright (c) 2017 Thaylon Guedes Santos and Eduardo Smil Prutchi

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package br.uff.scicumulus2prov.core

import br.uff.scicumulus2prov.model.*
import org.sql2o.data.Table
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author Thaylon Guedes Santos
 * @email thaylongs@gmail.com
 */
data class DerivedFromResultQuery(val tableTo: String, val fromData: Map<String, List<String>>)

class ConceptualDataDAO(val dao: BasicDao) {

    fun findCWorkflowByTAG(tag: String): CWorkflow {
        return dao.executeAndFetchFirst("SELECT * FROM cworkflow WHERE tag = :tag LIMIT 1", mapOf("tag" to tag), CWorkflow::class.java)
    }

    fun findCActivitiesByWkfid(wkfid: Long): List<CActivity> {
        return dao.executeAndFetch("SELECT * FROM cactivity WHERE wkfid = :wkfid", mapOf("wkfid" to wkfid), CActivity::class.java)
    }

    fun getAllFieldOfActovity(actID: Long): List<CActivityField> {
        return dao.executeAndFetch(
                """SELECT DISTINCT
                    f.fname,
                    f.ftype
                   FROM
                    cfield f INNER JOIN crelation r ON r.relid = f.relid
                        INNER JOIN cactivity a ON r.actid = :id""", mapOf("id" to actID), CActivityField::class.java)
    }

    fun getAllRelation(cWorkflow: CWorkflow): List<String> {
        return dao.executeAndFetch("SELECT DISTINCT  r.rname FROM crelation r INNER JOIN cactivity act ON r.actid = act.actid INNER JOIN cworkflow workflow ON act.wkfid = workflow.wkfid WHERE workflow.wkfid=:id",
                mapOf("id" to cWorkflow.wkfid), String::class.java)
    }

    fun getAllDerivedOutputTablesValuesFromInputTables(eActivity: EActivity): Optional<DerivedFromResultQuery> {

        val query = """
                        SELECT *
                        FROM (
                               SELECT
                                 relation.rname AS input_table,
                                 output.rname   AS output_table,
                                 field.fname    AS field_name
                               FROM
                                 crelation relation INNER JOIN cfield field ON relation.relid = field.relid
                                 INNER JOIN (
                                              SELECT
                                                field.fname,
                                                relation.rname
                                              FROM cfield field INNER JOIN crelation relation ON field.relid = relation.relid
                                              WHERE relation.rtype = 'OUTPUT' AND relation.actid = :actid
                                            ) AS output ON field.fname = output.fname
                               WHERE
                                 relation.rtype = 'INPUT' AND relation.actid = :actid
                             ) AS result
                    """
        val resultQuery = dao.executeAndFetchTable(query, mapOf("actid" to eActivity.cactid))
        if (resultQuery.rows().isEmpty()) return Optional.empty()
        var outputTableName: String = ""
        val map = hashMapOf<String, ArrayList<String>>()
        resultQuery.rows().forEach { linha ->
            outputTableName = linha.getString("output_table")
            val inputTable = linha.getString("input_table")
            val fieldName = linha.getString("field_name")
            when (inputTable) {
                in map -> map[inputTable]!!.add(fieldName)
                else -> map[inputTable] = arrayListOf(fieldName)
            }
        }
        return Optional.of(DerivedFromResultQuery(outputTableName, map))
    }

    fun getAllDerivedInputTablesValuesFromOutputTables(eActivity: EActivity): Optional<DerivedFromResultQuery> {
        val query = """
                        SELECT *
                        FROM (
                               SELECT
                                 relation.rname AS input_table,
                                 output.rname   AS output_table,
                                 field.fname    AS field_name
                               FROM
                                 crelation relation INNER JOIN cfield field ON relation.relid = field.relid
                                 INNER JOIN (
                                              SELECT
                                                field.fname,
                                                relation.rname
                                              FROM cfield field INNER JOIN crelation relation ON field.relid = relation.relid
                                              WHERE relation.rtype = 'OUTPUT' AND relation.actid = :actid
                                            ) AS output ON field.fname = output.fname
                               WHERE
                                 relation.dependency = :actid
                             ) AS result
                    """
        val resultQuery = dao.executeAndFetchTable(query, mapOf("actid" to eActivity.cactid))
        if (resultQuery.rows().isEmpty()) return Optional.empty()
        var outputTableName: String = ""
        val map = hashMapOf<String, ArrayList<String>>()
        resultQuery.rows().forEach { linha ->
            outputTableName = linha.getString("input_table")
            val inputTable = linha.getString("output_table")
            val fieldName = linha.getString("field_name")
            when (inputTable) {
                in map -> map[inputTable]!!.add(fieldName)
                else -> map[inputTable] = arrayListOf(fieldName)
            }
        }
        return Optional.of(DerivedFromResultQuery(outputTableName, map))

    }

    fun getExecutionsIDsFromInputTablesToOutputTables(eworkflow: EWorkflow, outputTableName: String, inputTableName: String): List<Array<Long>> {
        val query = """
                        SELECT
                          input.ik  AS input_ik,
                          output.ok AS output_ok
                        FROM "${eworkflow.tag}"."$inputTableName" input INNER JOIN "${eworkflow.tag}"."$outputTableName" output
                            ON input.ewkfid = output.ewkfid AND input.ik = output.ik
                        WHERE input.ewkfid = :ewkfid
                    """
        val resultQuey = dao.executeAndFetchTable(query, mapOf("ewkfid" to eworkflow.ewkfid))
        return resultQuey.rows().map { arrayOf(it.getLong("input_ik"), it.getLong("output_ok")) }
    }

    fun getExecutionsIDsFromOutpuTablesToInputTables(eworkflow: EWorkflow, outputTableName: String, inputTableName: String): List<Array<Long>> {
        val query = """
                        SELECT
                          input.ik  AS input_ik,
                          output.ok AS output_ok,
                          output.ik AS output_ik
                        FROM "${eworkflow.tag}"."$inputTableName" input INNER JOIN "${eworkflow.tag}"."$outputTableName" output
                            ON input.ewkfid = output.ewkfid AND input.ik = output.ok
                        WHERE input.ewkfid = :ewkfid
                    """
        val resultQuey = dao.executeAndFetchTable(query, mapOf("ewkfid" to eworkflow.ewkfid))
        return resultQuey.rows().map { arrayOf(it.getLong("input_ik"), it.getLong("output_ok"), it.getLong("output_ik") ) }
    }

}