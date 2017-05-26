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

import br.uff.scicumulus2prov.BasicInformation
import br.uff.scicumulus2prov.model.CWorkflow
import br.uff.scicumulus2prov.model.EActivity
import br.uff.scicumulus2prov.model.EWorkflow
import java.io.File

/**
 * @author Thaylon Guedes Santos
 * @email thaylongs@gmail.com
 */
class SciCumulus2PROV(val basicInfo: BasicInformation, fileOut: File) {

    private val dao = BasicDao(basicInfo)
    private val conceptualDao = ConceptualDataDAO(dao)
    private val executionDao = ExecutionDataDAO(dao)
    private val document = DocumentBuilder(fileOut)

    fun start() {
        val workflow = conceptualDao.findCWorkflowByTAG(basicInfo.workflowTag)
        val eworkflow = executionDao.findEWorkflowByTagAndExecTag(basicInfo.workflowTag, basicInfo.execTag)
        val atividades = conceptualDao.findCActivitiesByWkfid(workflow.wkfid)

        val eAtividades = atividades.map {
            val executionInfo = executionDao.findEActivityByActAndEWorkflow(it, eworkflow)
            val act = document.newActicity("a" + executionInfo.actid, it.tag, executionInfo.starttime, executionInfo.endtime)
            act.setType(it.atype)
            conceptualDao.getAllFieldOfActovity(it.actid).forEach { field ->
                act.addAttribute("column", field.fname, AttributeType.valueOf(field.ftype.toUpperCase()))
            }
            document.writeElement(act)
            executionInfo
        }

        eAtividades.forEach { loadWasInformedBy(it, eworkflow) }
        loadAllEntities(workflow, eworkflow)
        loadAllwasGeneratedBy(eworkflow)
        loadAllwasUsedBy(eworkflow)
        loadAllwasDerivedFrom(eworkflow, eAtividades)
        document.finishDocument()
    }

    private fun loadAllwasDerivedFrom(eworkflow: EWorkflow, eAtividades: List<EActivity>) {
        eAtividades.forEach { eAct ->
            val resultFromInputTableToOutputTable = conceptualDao.getAllDerivedOutputTablesValuesFromInputTables(eAct)
            resultFromInputTableToOutputTable.ifPresent {
                val tablesInfo = resultFromInputTableToOutputTable.get()
                val outputTableName = tablesInfo.toTable
                tablesInfo.fromData.forEach { inputTableName, fields ->
                    val idsData = conceptualDao.getExecutionsIDsFromInputTablesToOutputTables(eworkflow, outputTableName, inputTableName)
                    for (ids in idsData) {
                        val actID = "a" + eAct.actid
                        val fromEntityId = "${inputTableName}_${eworkflow.ewkfid}_${ids[0]}"
                        val toEntityId = "${outputTableName}_${eworkflow.ewkfid}_${ids[0]}_${ids[1]}"
                        val wasDerived = document.newwasDerivedFrom(fromEntityId, toEntityId, actID, fields)
                        document.writeElement(wasDerived)
                    }
                }
            }

            val resultFromOutputTableToInputTable = conceptualDao.getAllDerivedInputTablesValuesFromOutputTables(eAct)
            resultFromOutputTableToInputTable.ifPresent {
                val tablesInfo = resultFromOutputTableToInputTable.get()
                val outputTableName = tablesInfo.fromTable
                tablesInfo.toData.forEach { inputTableName, fields ->
                    val idsData = conceptualDao.getExecutionsIDsFromOutpuTablesToInputTables(eworkflow = eworkflow, outputTableName = outputTableName, inputTableName = inputTableName)
                    for (ids in idsData) {
                        val actID = "a" + eAct.actid
                        val input_ik = ids[0]
                        val output_ok = ids[1]
                        val output_ik = ids[2]
                        val fromEntityId = "${outputTableName}_${eworkflow.ewkfid}_${output_ik}_$output_ok"
                        val toEntityId = "${inputTableName}_${eworkflow.ewkfid}_$input_ik"
                        val wasDerived = document.newwasDerivedFrom(fromEntityId, toEntityId, actID, fields)
                        document.writeElement(wasDerived)
                    }
                }
            }
        }
    }

    private fun loadAllwasGeneratedBy(eworkflow: EWorkflow) {
        val result = executionDao.getAllOutputTables(eworkflow)
        result.rows().forEach { linha ->
            val rname = linha.getString("rname")
            val actid = linha.getString("actid")
            executionDao.getAllExecutionIDsOF(rname, eworkflow, true).rows().forEach { ids ->
                val id = "${rname}_${eworkflow.ewkfid}_${ids.getObject("ik")}_${ids.getObject("ok")}"
                val wasGeneratedBy = document.newWasGeneratedBy(id, "a" + actid)
                document.writeElement(wasGeneratedBy)
            }
        }
    }

    private fun loadAllwasUsedBy(eworkflow: EWorkflow) {
        val result = executionDao.getAllInputTables(eworkflow)
        result.rows().forEach { linha ->
            val rname = linha.getString("rname")
            val actid = linha.getString("actid")
            executionDao.getAllExecutionIDsOF(rname, eworkflow, false).rows().forEach { ids ->
                val id = "${rname}_${eworkflow.ewkfid}_${ids.getObject("ik")}"
                val wasUsedBy = document.newWasUseddBy(id, "a" + actid)
                document.writeElement(wasUsedBy)
            }
        }
    }

    private fun loadAllEntities(workflow: CWorkflow, eworkflow: EWorkflow) {
        val relations = conceptualDao.getAllRelation(workflow)
        relations.forEach { relation ->
            val table = executionDao.getAllDataFrom(eworkflow, relation)
            val colunasValidas = table.columns().filter { it.name != "ik" && it.name != "ewkfid" && it.name != "ok" }
            val containsOkColumn = table.columns().any { it.name == "ok" }
            for (linha in table.rows()) {
                var id = "${relation}_${eworkflow.ewkfid}_${linha.getObject("ik")}"
                if (containsOkColumn) {
                    id += "_" + linha.getObject("ok")
                }
                val entity = document.addEntity(id)
                for (coluna in colunasValidas) {
                    entity.addAttribute(coluna.name, linha.getString(coluna.index), null)
                }
                document.writeElement(entity)
            }
        }
    }

    private fun loadWasInformedBy(act: EActivity, eworkflow: EWorkflow) {
        val relations = executionDao.getActivitieDependency(act, eworkflow)
        relations.forEach { document.writeElement(document.newWasInformedBy("a" + it, "a" + act.actid)) }
    }
}
