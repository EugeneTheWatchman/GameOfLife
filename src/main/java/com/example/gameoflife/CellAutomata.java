package com.example.gameoflife;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.*;

public class CellAutomata extends Application {

    private final int wight = 1280;
    private final int height = 720;
    private final int cellSize = 10;
    private final int xBound = this.wight / this.cellSize;
    private final int yBound = this.height / this.cellSize;
    private double mouseX;
    private double mouseY;
    private double simulationSpeed = 2;
    private Circle circle;
    private Slider slider;
    private int fillSize = 1;
    private Timeline timeline;
    private List<List<Rectangle>> cellGrid;
    private List<List<Boolean>> curDataGrid;
    private List<List<Boolean>> nextDataGrid;


    @Override
    public void start(Stage stage) {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> onUpdate()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.setRate(simulationSpeed);

        Timeline drawTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> draw()));
        drawTimeline.setCycleCount(Animation.INDEFINITE);
        drawTimeline.setRate(60);
        drawTimeline.play();


        var scene = new Scene(createContent());
        stage.setScene(scene);
        stage.show();
    }

    private void createMouseHandler(Pane root) {
        circle = new Circle(cellSize/sqrt(2), Color.rgb(255,255,0,0.5));
        root.getChildren().add(circle);
        root.setOnMouseMoved(mouseEvent -> {
            mouseX = mouseEvent.getX();
            mouseY = mouseEvent.getY();
        });
        root.setOnMouseClicked( mouseEvent -> {
            mouseX = mouseEvent.getX();
            mouseY = mouseEvent.getY();

            int x = (int) (mouseX/cellSize);
            int y = (int) (mouseY/cellSize);
            int new_y, new_x;

            MouseButton mouseButton = mouseEvent.getButton();
            int radius = fillSize;
            for (int i = -radius; i < radius; i++) {
                new_y = floorMod(y+i, yBound);

                for (int j = -radius; j < radius; j++) {
                    if (sqrt(j*j + i*i)*sqrt(2) >= radius)
                        continue;
                    new_x = floorMod(x+j,xBound);



                    if (mouseButton == MouseButton.PRIMARY) {
                        this.curDataGrid.get(new_y).set(new_x, Boolean.TRUE);
                    } else if (mouseButton == MouseButton.SECONDARY) {
                        this.curDataGrid.get(new_y).set(new_x, Boolean.FALSE);
                    }
                }
            }

        });
        root.setOnMouseDragged(root.getOnMouseClicked());
        root.setOnScroll(scrollEvent -> {

            if (scrollEvent.isControlDown()) {
                slider.setValue(slider.getValue() + (scrollEvent.getDeltaY() > 0 ? 2 : -2));
            } else

            if (scrollEvent.getDeltaY() > 0) {
                fillSize += 1;
            } else {
                fillSize = max(fillSize - 1, 1);
            }

            circle.setRadius(fillSize*cellSize/sqrt(2));
        });
    }

    private Parent createContent() {

        var simView = this.createSimulationView();
        VBox.setVgrow(simView, Priority.ALWAYS);
        this.createMouseHandler((Pane) simView);

        var intfView = this.createInterfaceView();

        VBox main = new VBox();
        main.getChildren().addAll(simView, intfView);
        return main;
    }

    private Parent createSimulationView() {
        Pane pane = new Pane();
        pane.setPrefSize(this.wight, this.height);
        pane.setBackground(new Background(new BackgroundFill(Color.GRAY, null, null)));
        this.createGrid(pane);

        return pane;
    }

    private Parent createInterfaceView() {
        var textOn = "Stop";
        var textOff = "Start";
        ToggleButton toggleRunButton = new ToggleButton(textOff);
        toggleRunButton.setOnAction(actionEvent -> {
            if (toggleRunButton.isSelected()) { // положение старт
                timeline.play();
                toggleRunButton.setText(textOn);
            } else { // положение стоп
                timeline.pause();
                toggleRunButton.setText(textOff);

            }
        });

        slider = new Slider(0, 100, simulationSpeed);
        slider.setShowTickMarks(true);
        slider.setOrientation(Orientation.HORIZONTAL);
        slider.setPrefWidth(100);
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            simulationSpeed = (Double) newValue;
            timeline.setRate(simulationSpeed);
        });

        HBox hBox = new HBox(10);
        hBox.getChildren().addAll(toggleRunButton, slider);
        return hBox;
    }

    private void createGrid(Pane root) {
        this.cellGrid = new ArrayList<>();
        this.curDataGrid = new ArrayList<>();
        Random random = new Random();
        for (int y = 0; y < this.height / this.cellSize; y++) {
            List<Rectangle> cellRow = new ArrayList<>();
            List<Boolean> curDataRow = new ArrayList<>();
            for (int x = 0; x < this.wight / this.cellSize; x++) {
                var rect = new Rectangle(this.cellSize-1, this.cellSize-1);
                rect.setX(x * this.cellSize);
                rect.setY(y * this.cellSize);
                root.getChildren().add(rect);
                cellRow.add(rect);

                // пока что случайное заполнение
                curDataRow.add( random.nextDouble() < .15 );
            }
            this.cellGrid.add(cellRow);
            this.curDataGrid.add(curDataRow);
        }
        this.nextDataGrid = this.curDataGrid.stream()
                .map(ArrayList::new)
                .collect(Collectors.toList());
    }

    private void onUpdate() {

        int xBound = this.wight / this.cellSize;
        int yBound = this.height / this.cellSize;
        // TODO надо оптимизировать:
        //  обходить только живые клетки и её соседей
        //  записывать соседей, которых уже обошли, чтобы не повторяться
        for (int y = 0; y < yBound; y++) {
            for (int x = 0; x < xBound; x++) {
                Boolean value = applyRule(x, y);
                nextDataGrid.get(y).set(x, value);
            }
        }
        var temp = this.curDataGrid;
        this.curDataGrid = this.nextDataGrid;
        this.nextDataGrid = temp;
    }

    private boolean applyRule(int x, int y) {
        var topRow    = this.curDataGrid.get(floorMod(y-1,yBound));
        var middleRow = this.curDataGrid.get(y);
        var bottomRow = this.curDataGrid.get(floorMod(y+1,yBound));

        var neighboursAlive = Stream.of(
                topRow.get(floorMod(x-1,xBound)),       topRow.get(x),      topRow.get(floorMod(x+1,xBound)),
                middleRow.get(floorMod(x-1,xBound)),                        middleRow.get(floorMod(x+1,xBound)),
                bottomRow.get(floorMod(x-1,xBound)),    bottomRow.get(x),   bottomRow.get(floorMod(x+1,xBound))
        ).filter(value -> value).count();

        // if current cell alive
        if (middleRow.get(x)) {
            return neighboursAlive == 2 || neighboursAlive == 3;
        }

        // ANY cell with excactly three live neighbours comes/continues to live
        return neighboursAlive == 3;
    }

    private void draw() {
        circle.setCenterX(mouseX);
        circle.setCenterY(mouseY);

        for (int y = 0; y < yBound; y++) {
            for (int x = 0; x < xBound; x++) {

                if (this.curDataGrid.get(y).get(x)) {
                    this.cellGrid.get(y).get(x).setFill(Color.WHITE);
                } else {
                    this.cellGrid.get(y).get(x).setFill(Color.BLACK);
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
