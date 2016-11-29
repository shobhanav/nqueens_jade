package queens;

import jade.util.leap.Serializable;

@SuppressWarnings("serial")
public class Position implements Serializable{
	
	private int row;
	private int column;
	
	public Position(int row, int column){
		this.row = row;
		this.column = column;
	}

	public int getRow() {
		return row;
	}

	public int getColumn() {
		return column;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "[Row: " + row + ", Column: " + column +"]";
	}
	
	
	
	

}
