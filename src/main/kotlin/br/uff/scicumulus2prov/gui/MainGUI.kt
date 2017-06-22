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
package br.uff.scicumulus2prov.gui

import br.uff.scicumulus2prov.core.BasicDao
import br.uff.scicumulus2prov.core.ExecutionDataDAO
import br.uff.scicumulus2prov.core.SciCumulus2PROV
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.stage.FileChooser
import java.net.URL
import java.util.*


/**
 * @author Thaylon Guedes Santos
 * @email thaylongs@gmail.com
 */
class MainGUI : Initializable {

    @FXML private lateinit var hostNameInputTx: TextField
    @FXML private lateinit var dbNameInputTx: TextField
    @FXML private lateinit var dbPortInputTx: TextField
    @FXML private lateinit var userNameInpuTx: TextField
    @FXML private lateinit var passwordInputTx: PasswordField
    @FXML private lateinit var conectarBtn: Button
    @FXML private lateinit var exportarBtn: Button
    //List
    @FXML private lateinit var workflowsList: ListView<String>
    @FXML private lateinit var execucoesList: ListView<String>
    private var basicDao: BasicDao? = null
    private var executionDao: ExecutionDataDAO? = null


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        conectarBtn.setOnMouseClicked { onClickConectar() }
        exportarBtn.setOnMouseClicked { onClickExportar() }
        workflowsList.selectionModel.selectedItemProperty().addListener({ observable, oldValue, newValue -> if (newValue != null) onSelectWorkflow(newValue) })
    }

    fun onClickConectar() {
        basicDao = BasicDao(dbHostname = hostNameInputTx.text,
                dbName = dbNameInputTx.text,
                dbPort = dbPortInputTx.text.toInt(),
                dbPassword = passwordInputTx.text,
                dbUsername = userNameInpuTx.text
        )
        workflowsList.items.clear()
        execucoesList.items.clear()
        executionDao = ExecutionDataDAO(basicDao!!)
        workflowsList.items = FXCollections.observableArrayList(executionDao!!.getAllWorkflows())
    }


    private fun onSelectWorkflow(workflowTag: String) {
        execucoesList.items = FXCollections.observableArrayList(executionDao!!.getAllExecutionOfWorkflows(workflowTag))
    }

    fun onClickExportar() {
        val execTag = execucoesList.selectionModel.selectedItem
        if (execTag == null) {
            //TODO
            return
        }
        val fileChooser = FileChooser()
        fileChooser.title = "Save W3C PROV-N File"
        fileChooser.extensionFilters.addAll(
                FileChooser.ExtensionFilter("W3C Prov-N", "*.provn"),
                FileChooser.ExtensionFilter("All Files", "*.*"))
        val selectedFile = fileChooser.showSaveDialog(execucoesList.scene.window)
        val workflowTag = workflowsList.selectionModel.selectedItem
        if (execTag != null && workflowTag != null && selectedFile != null) {
            if (selectedFile.exists()) selectedFile.delete()
            SciCumulus2PROV(dao = basicDao!!, execTag = execTag, fileOut = selectedFile, workflowTag = workflowTag).start()
            val dialog = Alert(Alert.AlertType.INFORMATION)
            dialog.title = "Success"
            dialog.headerText = "The provenance file was saved with success"
            dialog.contentText = "Location: ${selectedFile.absolutePath}"
            dialog.showAndWait()
        }
    }
}