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

import org.sql2o.Sql2o
import org.sql2o.data.Table

/**
 * @author Thaylon Guedes Santos
 * @email thaylongs@gmail.com
 */
class BasicDao(dbName: String,
               dbPassword: String,
               dbUsername: String,
               dbHostname: String,
               dbPort: Int) {

    private val sql2o: Sql2o

    init {
        var url = "jdbc:postgresql://$dbHostname:$dbPort/$dbName"
        sql2o = Sql2o(url, dbUsername, dbPassword)
    }

    fun <R> executeAndFetch(query: String, parameter: Map<String, Any>, returnType: Class<R>): List<R> {
        return sql2o.open().use { con ->
            val queryBuilder = con.createQuery(query)
            for ((key, value) in parameter) {
                queryBuilder.addParameter(key, value)
            }
            return queryBuilder.executeAndFetch(returnType)
        }
    }

    fun <R> executeAndFetchFirst(query: String, parameter: Map<String, Any>, returnType: Class<R>): R {
        return sql2o.open().use { con ->
            val queryBuilder = con.createQuery(query)
            for ((key, value) in parameter) {
                queryBuilder.addParameter(key, value)
            }
            return queryBuilder.executeAndFetchFirst(returnType)
        }
    }


    fun executeAndFetchTable(query: String, parameter: Map<String, Any>): Table {
        return sql2o.open().use { con ->
            val queryBuilder = con.createQuery(query)
            for ((key, value) in parameter) {
                queryBuilder.addParameter(key, value)
            }
            return queryBuilder.executeAndFetchTable()
        }
    }
}
