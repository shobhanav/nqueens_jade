package queens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;


import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREInitiator;

public class ChessBoard extends Agent{

	private int n;	
	
	ArrayList<Position> map = new ArrayList<Position>();
	@Override
	protected void setup() {
		Object[] args = getArguments();
		if(args == null || args.length == 0){
			System.out.println("<" + getLocalName() + "> not invoked with size of the board. Exiting...");
			doDelete();
			return;
		}
		System.out.println("Chess board ready");
		n = Integer.parseInt((String) args[0]);
		
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();		
		DFAgentDescription[] result = null;
		sd.setType("queen");
		dfd.addServices(sd);			
		
		try {
			result = DFService.search(this, dfd);
		} catch (FIPAException ex) {
			ex.printStackTrace();
		}		
		SequentialBehaviour sb = new SequentialBehaviour();	
		ParallelBehaviour pb = new ParallelBehaviour();
		
		
		for(DFAgentDescription agent : result) {
			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			request.addReceiver(agent.getName());
			request.setContent("getFinalPosition");
			pb.addSubBehaviour(new FinalSolutionRequester(this, request));
		}
		sb.addSubBehaviour(pb);
		sb.addSubBehaviour(new PrintBoard());
		addBehaviour(sb);		
	}
	

	@Override
	protected void takeDown() {
		
	}
	
	@SuppressWarnings("serial")
	private class FinalSolutionRequester extends SimpleAchieveREInitiator {

		public FinalSolutionRequester(Agent a, ACLMessage msg) {
			super(a, msg);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void handleInform(ACLMessage msg) {
			Position pos;
			try {
				pos = (Position) msg.getContentObject();				
				map.add(pos);
				
			} catch (UnreadableException e) {				
				e.printStackTrace();
			}
		}
				
	}
	
	private class PrintBoard extends OneShotBehaviour {

		@Override
		public void action() {
			//traverse through Map
			int[][] chess = new int[n][n];
			for(Position pos : map){
				chess[pos.getRow()][pos.getColumn()] = 1;			
			}
			
			System.out.println("-------Final Solution----------");
			for(int i = 0 ; i <n ; ++i){
				for(int j =0; j<n; ++j){
					System.out.print(String.format("%2s", chess[i][j]));
				}
				System.out.println("");
			}
			
		}
		
	}
}
