package client.scenes;

import com.google.inject.Inject;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class WebViewCtrl implements Initializable {

    private final MainCtrl mainCtrl;

    @FXML
    private WebView webView;

    @FXML
    private Button backButton;

    @FXML
    private AnchorPane pane;

    private WebEngine engine;
    private String page;

    @Inject
    public WebViewCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

    /**
     * {@inheritDoc}
     */
    public void initialize(URL location, ResourceBundle resources) {
        webView.prefWidthProperty().bind(pane.widthProperty());
        webView.prefHeightProperty().bind(pane.heightProperty());

        pane.widthProperty().addListener((ObservableValue<? extends Number>
                                                  observable, Number oldValue, Number newValue) -> {
            backButton.setLayoutX(newValue.doubleValue() / 2 - (backButton.widthProperty().getValue() / 2));
        });

        engine = webView.getEngine();
        page = "http://localhost:8080/";
        loadPage();
    }

    /**
     * Loads the edit activities page.
     */
    public void loadPage() {
        engine.load(page);
    }

    /**
     * Revert the user back to the splash screen.
     */
    public void back() {
        mainCtrl.showSplash();
    }
}
