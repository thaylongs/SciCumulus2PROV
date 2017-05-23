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

/**
 * Created by thaylon on 16/05/17.
 */
open class ConceptualDataDAO(val dao: BasicDao) {

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

}