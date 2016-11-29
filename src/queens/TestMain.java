package queens;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.core.Timer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

public class TestMain {

	/**
	 * SET THIS VARIABLE TO SET THE BOARD SIZE
	 */
	private final static int CHESS_BOARD_SIZE = 8;
	
	
	
	
	private static TestMain instance = null;
	private static AgentController controller;
	

	protected TestMain() throws ControllerException {
		
		jade.core.Runtime rt = jade.core.Runtime.instance();

		Profile profile = new ProfileImpl();
		jade.wrapper.AgentContainer mainContainer = rt.createMainContainer(profile);
		
		for(int i =0; i<CHESS_BOARD_SIZE; ++i) {
			Object[] args = new Object[2];
			args[0] = String.valueOf(i);
			args[1] = String.valueOf(CHESS_BOARD_SIZE-1);
			controller = mainContainer.createNewAgent("q"+i, "queens.Queen", args);
			controller.start();
		}
		
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Object[] a = new Object[1];
		
		a[0] = String.valueOf(CHESS_BOARD_SIZE);
		try {
			controller = mainContainer.createNewAgent("chess", "queens.ChessBoard", a);
			controller.start();
		} catch (StaleProxyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static TestMain getInstance() throws ControllerException {
		if (instance == null) {
			instance = new TestMain();
		}

		return instance;
	}

	public static void killInstance() throws StaleProxyException {
		controller.kill();
		instance = null;
	}
	
public static void main(String[] args) throws StaleProxyException {
		
		TestMain controller;
		try {
			controller = TestMain.getInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//TestMain.killInstance();
	}
}
