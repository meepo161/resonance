package ru.avem.resonance.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import ru.avem.resonance.communication.devices.pr200.OwenPRModel;

import java.util.Observable;
import java.util.Observer;

import static ru.avem.resonance.communication.devices.DeviceController.PR200_ID;

public class CurrentProtection implements Observer {
    @FXML
    private Circle deviceStateKM1;
    @FXML
    private Circle deviceStateKM2;
    @FXML
    private Circle deviceStateKM3;
    @FXML
    private Circle deviceStateKM4;
    @FXML
    private Circle deviceStateKM5;
    @FXML
    private Circle deviceStateKM6;
    @FXML
    private Circle deviceStateKM7;
    @FXML
    private Circle deviceStateKM8;
    @FXML
    private Circle deviceStateKM9;
    @FXML
    private Circle deviceStateKM10;
    @FXML
    private Circle deviceStateKM11;
    @FXML
    private Circle deviceStateKM12;
    @FXML
    private Circle deviceStateKM13;
    @FXML
    private Circle deviceStateKM14;
    @FXML
    private Circle deviceStateKM15;

    @Override
    public void update(Observable o, Object values) {
        int modelId = (int) (((Object[]) values)[0]);
        int param = (int) (((Object[]) values)[1]);
        Object value = (((Object[]) values)[2]);

        if (modelId == PR200_ID) {
            switch (param) {
                case OwenPRModel.РУЧНОЙ_РЕЖИМ_С_ПК:
                    Platform.runLater(() -> deviceStateKM1.setFill(((boolean) value) ? Color.LIME : Color.RED));
                    break;
                case OwenPRModel.ПЕРЕМЕННОЕ:
                    Platform.runLater(() -> deviceStateKM2.setFill(((boolean) value) ? Color.LIME : Color.RED));
                    break;
                case OwenPRModel.ПЕРЕМЕННОЕ_С_РЕЗОНАНСОМ:
                    Platform.runLater(() -> deviceStateKM3.setFill(((boolean) value) ? Color.LIME : Color.RED));
                    break;
                case OwenPRModel.ПОСТОЯННОЕ:
                    Platform.runLater(() -> deviceStateKM4.setFill(((boolean) value) ? Color.LIME : Color.RED));
                    break;
                case OwenPRModel.КОНТРОЛЬ_РУБИЛЬНИКА:
                    Platform.runLater(() -> deviceStateKM5.setFill(((boolean) value) ? Color.LIME : Color.RED));
                    break;
                case OwenPRModel.СТАРТ_ТАЙМЕР:
                    Platform.runLater(() -> deviceStateKM6.setFill(((boolean) value) ? Color.LIME : Color.RED));
                    break;
                case OwenPRModel.СТОП_ТАЙМЕР:
                    Platform.runLater(() -> deviceStateKM7.setFill(((boolean) value) ? Color.LIME : Color.RED));
                    break;
                case OwenPRModel.КОНТРОЛЬ_ПУСКА:
                    Platform.runLater(() -> deviceStateKM8.setFill(((boolean) value) ? Color.LIME : Color.RED));
                    break;
                case OwenPRModel.ТКЗ_ДО_ТРАНСФОРМАТОРА:
                    Platform.runLater(() -> deviceStateKM9.setFill(((boolean) value) ? Color.RED : Color.LIME));
                    break;
                case OwenPRModel.КОНТРОЛЬ_ДВЕРЕЙ_ШСО:
                    Platform.runLater(() -> deviceStateKM10.setFill(((boolean) value) ? Color.RED : Color.LIME));
                    break;
                case OwenPRModel.ТКЗ_ОИ:
                    Platform.runLater(() -> deviceStateKM11.setFill(((boolean) value) ? Color.RED : Color.LIME));
                    break;
                case OwenPRModel.ТКЗ_ПОСЛЕ_ТРАНСФОРМАТОРА:
                    Platform.runLater(() -> deviceStateKM12.setFill(((boolean) value) ? Color.RED : Color.LIME));
                    break;
                case OwenPRModel.СТОП_ИСПЫТАНИЯ:
                    Platform.runLater(() -> deviceStateKM13.setFill(((boolean) value) ? Color.LIME : Color.RED));
                    break;
                case OwenPRModel.ПОДЪЕМ_НАПРЯЖЕНИЯ:
                    Platform.runLater(() -> deviceStateKM14.setFill(((boolean) value) ? Color.LIME : Color.RED));
                    break;
                case OwenPRModel.УМЕНЬШЕНИЕ_НАПРЯЖЕНИЯ:
                    Platform.runLater(() -> deviceStateKM15.setFill(((boolean) value) ? Color.LIME : Color.RED));
                    break;
            }
        }
    }
}
