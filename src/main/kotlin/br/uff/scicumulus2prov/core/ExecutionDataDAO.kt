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
import br.uff.scicumulus2prov.model.EActivation
import br.uff.scicumulus2prov.model.EActivity
import br.uff.scicumulus2prov.model.EWorkflow
import org.sql2o.data.Table
import java.sql.Timestamp

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

    fun getEActivationsByEActivity(eactivity: EActivity): List<EActivation> {
        return dao.executeAndFetch("select * from eactivation where actid=:id", mapOf("id" to eactivity.actid), EActivation::class.java)
    }

    fun getAllUsedMachines(eWorkflow: EWorkflow): Table {
        val query = """
                        SELECT
                          emachine.machineid AS machine_id,
                          emachine.hostname,
                          emachine.address,
                          emachine.mflopspersecond,
                          eactivation.taskid
                        FROM emachine
                          INNER JOIN eactivation ON emachine.machineid = eactivation.machineid
                          INNER JOIN eactivity ON eactivation.actid = eactivity.actid
                        WHERE eactivity.wkfid = :id
                    """
        return dao.executeAndFetchTable(query, mapOf("id" to eWorkflow.ewkfid))
    }

    fun getKeySpaceData(eworkflow: EWorkflow): Table {
        val query = """
                        SELECT
                          ekeyspace.taskid,
                          ekeyspace.relationname,
                          ekeyspace.relationtype,
                          ekeyspace.fik
                        FROM ekeyspace
                          INNER JOIN eactivity ON eactivity.actid = ekeyspace.actid
                        WHERE eactivity.wkfid = :id
                    """
        return dao.executeAndFetchTable(query, mapOf("id" to eworkflow.ewkfid))
    }

    fun getAllWorkflows(): List<String> {
        return dao.executeAndFetch("select tag from cworkflow", mapOf(), String::class.java)
    }

    fun getAllExecutionOfWorkflows(workflowTag: String): List<String> {
        return dao.executeAndFetch("select tagexec from eworkflow where tag=:tag", mapOf("tag" to workflowTag), String::class.java)
    }

    fun getStartAndEndTimeOfExecutionWorkflow(eWorkflow: EWorkflow): Array<Timestamp> {
        val result = dao.executeAndFetchTable("SELECT min(act.starttime), max(act.endtime) FROM eactivity act WHERE act.wkfid = :id", mapOf("id" to eWorkflow.ewkfid))
        return arrayOf(result.rows()[0].getObject(0) as Timestamp, result.rows()[0].getObject(1) as Timestamp)
    }
}
