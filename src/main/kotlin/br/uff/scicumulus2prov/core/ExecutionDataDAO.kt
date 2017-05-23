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

import br.uff.scicumulus2prov.model.CActivity
import br.uff.scicumulus2prov.model.EActivity
import br.uff.scicumulus2prov.model.EWorkflow
import org.sql2o.data.Table

/**
 * @author Thaylon Guedes Santos
 * @email thaylongs@gmail.com
 */
class ExecutionDataDAO(val dao: BasicDao) {

    fun findEWorkflowByTagAndExecTag(workflowTag: String, execTag: String): EWorkflow {
        return dao.executeAndFetchFirst(
                "SELECT * FROM eworkflow WHERE tagexec=:execTag and tag=:workflowTag",
                mapOf("execTag" to execTag, "workflowTag" to workflowTag),
                EWorkflow::class.java)
    }

    fun findEActivityByActAndEWorkflow(act: CActivity, eWorkflow: EWorkflow): EActivity {
        return dao.executeAndFetchFirst(
                "SELECT * from eactivity WHERE wkfid=:wkfid and cactid=:cactid",
                mapOf("wkfid" to eWorkflow.ewkfid, "cactid" to act.actid),
                EActivity::class.java)
    }

    fun getAllDataFrom(eWorkflow: EWorkflow, tableName: String): Table {
        val query = "select * from ${eWorkflow.tag}.$tableName where ewkfid=:id"
        return dao.executeAndFetchTable(query, mapOf("id" to eWorkflow.ewkfid))
    }

    fun getAllOutputTables(eworkflow: EWorkflow): Table {
        val query = "SELECT r.rname, eact.actid FROM crelation r INNER JOIN eactivity eact ON eact.cactid = r.actid WHERE r.rtype = 'OUTPUT' AND eact.wkfid=:id ORDER BY eact.actid"
        return dao.executeAndFetchTable(query, mapOf("id" to eworkflow.ewkfid))
    }

    fun getAllInputTables(eworkflow: EWorkflow): Table {
        val query = "SELECT r.rname, eact.actid FROM crelation r INNER JOIN eactivity eact ON eact.cactid = r.actid WHERE r.rtype = 'INPUT' AND eact.wkfid=:id ORDER BY eact.actid"
        return dao.executeAndFetchTable(query, mapOf("id" to eworkflow.ewkfid))
    }

    fun getAllExecutionIDsOF(tableName: String, eWorkflow: EWorkflow, hasOkColumn: Boolean): Table {
        val okColumn = if(hasOkColumn) ",ok" else ""
        val query = "select ik $okColumn from ${eWorkflow.tag}.$tableName where ewkfid=:id"
        return dao.executeAndFetchTable(query, mapOf("id" to eWorkflow.ewkfid))
    }

    fun getActivitieDependency(eActivity: EActivity, eWorkflow: EWorkflow): List<String> {
        val query = "SELECT eact.actid FROM eactivity eact INNER JOIN crelation c ON eact.cactid = c.actid WHERE c.dependency = :actid AND eact.wkfid = :eWorkflowId"
        return dao.executeAndFetch(query, mapOf("actid" to eActivity.cactid, "eWorkflowId" to eWorkflow.ewkfid), String::class.java)
    }
}
