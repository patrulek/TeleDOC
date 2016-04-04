package com.pp.iwm.teledoc.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import com.pp.iwm.teledoc.objects.File;
import com.pp.iwm.teledoc.objects.FileTree;
import com.pp.iwm.teledoc.windows.AppWindow;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public class FileExplorer extends Pane {
	
	FileTree file_tree = null;
	AppWindow app_window = null;
	ScrollPane scroll_pane = null;
	Pane simple_pane = null;
	FileCard selected_card = null;
	FileCard hovered_card = null;
	ImageButton btn_back = null;
	Label lbl_path = null;
	List<FileCard> files = null;
	int max_cards_in_row = 0;
	double cards_gap = 20.0;
	
	public FileExplorer(AppWindow _app_window) {
		setPrefWidth(759.0);
		setPrefHeight(548.0);
		app_window = _app_window;
		
		
		files = new ArrayList<>();
		calcMaxCardsInRow();
		
		simple_pane = new Pane();
		simple_pane.setStyle("-fx-background-color: rgb(30, 54, 60, 1.0);");
		simple_pane.setPrefWidth(759.0);
		
		btn_back = new ImageButton(Utils.IMG_PARENT_FOLDER_SMALL, Utils.HINT_PARENT_FOLDER, Utils.ACT_PARENT_FOLDER);
		btn_back.setLayoutX(3.0); btn_back.setLayoutY(8.0);
		btn_back.customizeZoomAnimation(1.15, 1.0, 200, 200);
		btn_back.enableFadeAnimation(false);
		btn_back.addEventHandler(MouseEvent.MOUSE_ENTERED, ev -> onBtnMouseEntered(btn_back));
		btn_back.addEventHandler(MouseEvent.MOUSE_EXITED, ev -> onBtnMouseExited(btn_back));
		btn_back.addEventHandler(ActionEvent.ACTION, ev -> onBtnMouseClicked(btn_back));
		
		lbl_path = new Label("root/");
		lbl_path.setFont(Utils.LBL_STATUSBAR_FONT);
		lbl_path.setStyle("-fx-text-fill: rgb(160, 160, 200);");
		lbl_path.setMaxWidth(600.0);
		lbl_path.setLayoutX(52.0); lbl_path.setLayoutY(13.0);
		
		simple_pane.getChildren().add(btn_back);
		simple_pane.getChildren().add(lbl_path);
		
		
	
		scroll_pane = new ScrollPane(simple_pane);
		scroll_pane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scroll_pane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		scroll_pane.setLayoutY(42.0);
		scroll_pane.setPrefWidth(759.0); scroll_pane.setPrefHeight(493.0);
		scroll_pane.setStyle("-fx-background: rgb(30, 54, 60, 1.0); -fx-background-color: rgb(30, 54, 60, 1.0);");
		scroll_pane.setOnMouseClicked(event -> onScrollPaneMouseClicked());
		
		getChildren().add(scroll_pane);
		
		file_tree = new FileTree();
		refreshView();
	}
	
	public FileCard getSelectedCard() {
		return selected_card;
	}
	
	public void onCardSelect(FileCard _selected_card) {
		if( selected_card != null ) 
			selected_card.deselectCard();
		
		selected_card = _selected_card;
		selected_card.selectCard();
	}
	
	
	public void refreshView() {
		files.clear();
		simple_pane.getChildren().clear();
		simple_pane.getChildren().add(btn_back);
		simple_pane.getChildren().add(lbl_path);
		int j = 0;
		
		for( Entry<String, File> entry : file_tree.current_root.children.entrySet() ) {
			FileCard fc1 = new FileCard(this, entry.getValue());
			files.add(fc1);
		}
		
		java.util.Collections.sort(files, new Comparator<FileCard>() {
			@Override
			public int compare(FileCard fc1, FileCard fc2) {
				if( fc1.file.is_folder == fc2.file.is_folder ) 
					return fc1.file.name.compareTo(fc2.file.name);
				else {
					if( fc1.file.is_folder && !fc2.file.is_folder )
						return -1;
					else if( !fc1.file.is_folder && fc2.file.is_folder )
						return 1;
					return 0;
				}
			}
		});
		
		for( int i = 0; i < files.size(); i++ ) {
			double x = (i % max_cards_in_row) * 52.0 + 50.0;
			double y = (i / max_cards_in_row) * 80.0 + 40.0;
			
			simple_pane.getChildren().add(files.get(i));
			files.get(i).setLayoutX(x); files.get(i).setLayoutY(y);
		}
	}
	
	public void onCardChoose(FileCard _choosed_card) {
		String str = _choosed_card.lbl_name.getText();
		
		if( _choosed_card.file.is_folder )
			if( file_tree.current_root.children != null && !file_tree.current_root.children.isEmpty() ) {
				file_tree.current_root = file_tree.current_root.children.get(str);
				updateLabelPath();
				refreshView();
			}
		else
			; // wy�wietl plik
	}
	
	public void onCardHover(FileCard _hovered_card) {
		hovered_card = _hovered_card;
	}
	
	private void calcMaxCardsInRow() {
		max_cards_in_row = (int)(719.0 / (32.0 + cards_gap));
	}
	
	private void onScrollPaneMouseClicked() {
		if( selected_card != null && hovered_card == null) {
			selected_card.deselectCard();
			selected_card = null;
		}
	}
	
	private void onBtnMouseClicked(ImageButton _btn) {
		if( _btn == btn_back ) {
			if( file_tree.current_root.parent != null ) {
				if( selected_card != null ) {
					selected_card = null;
					app_window.status_bar.removeText();
				}
				
				file_tree.current_root = file_tree.current_root.parent;
				updateLabelPath();
				refreshView();
			}
		}
	}
	
	private void updateLabelPath() {
		lbl_path.setText(file_tree.current_root.path);
	}
	
	private void onBtnMouseEntered(ImageButton _btn) {
		if( _btn == btn_back )
			app_window.status_bar.addText(_btn.getHint());
	}
	
	private void onBtnMouseExited(ImageButton _btn) {
		app_window.status_bar.removeText();
	}
}
