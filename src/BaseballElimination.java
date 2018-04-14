import java.util.ArrayList;

import edu.princeton.cs.algs4.Bag;
import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.In;

public class BaseballElimination {

    private final int numOfTeams;
    private final String[] teamNames;
    private final int[] w; // wins
    private final int[] lose; // loses
    private final int[] r; // remaining games
    private final int[][] g; // games left to play for each pair of team

    private final int numOfV; // number of vertices in flow network

    private final boolean[] isEliminated;
    private final ArrayList<Bag<String>> certificateOfElimination;

    // create a baseball division from given filename in format specified below
    public BaseballElimination(String filename) {
        In in = new In(filename);
        this.numOfTeams = in.readInt();
        in.readLine();
        teamNames = new String[numOfTeams];

        int teamVertices = numOfTeams - 1; // 'teamVertices' is a temp vertices to help calculate gameVertices
        int gameVertices = (teamVertices * (teamVertices - 1)) / 2;
        this.numOfV = gameVertices + numOfTeams + 2; // add the dummy x vertices but no edges connected to it

        // initialize arrays
        this.w = new int[numOfTeams];
        this.lose = new int[numOfTeams];
        this.r = new int[numOfTeams];
        this.g = new int[numOfTeams][numOfTeams];
        // fullfill arrays
        for (int i = 0; i < numOfTeams; i++) {
            String line = in.readLine();
            if (line != null) {
                line = line.trim();
                String[] items = line.split(" +");
                int index = 0;
                teamNames[i] = items[index++];
                w[i] = Integer.parseInt(items[index++]);
                lose[i] = Integer.parseInt(items[index++]);
                r[i] = Integer.parseInt(items[index++]);
                for (int j = 0; j < numOfTeams; j++) {
                    g[i][j] = Integer.parseInt(items[j + index]);
                }
            }
        }

        this.isEliminated = new boolean[numOfTeams];
        // initial false
        for (int i = 0; i < numOfTeams; i++) {
            isEliminated[i] = false;
        }
        // initial bag to null
        this.certificateOfElimination = new ArrayList<Bag<String>>();
        for (int j = 0; j < numOfTeams; j++) {
            certificateOfElimination.add(null);
        }
        // run alg to calculate isEliminated and certificateOfElimination
        int maxWinIndex = calMaxWin();

        for (int x = 0; x < numOfTeams; x++) {
            if (w[x] + r[x] < w[maxWinIndex]) {
                // Trivial elimination
                isEliminated[x] = true;
                Bag<String> bag = new Bag<>();
                bag.add(teamNames[maxWinIndex]);
                certificateOfElimination.set(x, bag);
            } else {
                // Nontrivial elimination
                FlowNetwork fn = constructFN(x);
                FordFulkerson alg = new FordFulkerson(fn, numOfV - 2, numOfV - 1);
                // capture incut, for certificateOfElimination
                for (int k = 0; k < numOfTeams; k++) {
                    // if (k == x) continue;
                    if (alg.inCut(k)) {
                        // team x could be eliminated
                        isEliminated[x] = true;
                        String team = teamNames[k];
                        if (certificateOfElimination.get(x) == null) {
                            Bag<String> bag = new Bag<>();
                            bag.add(team);
                            certificateOfElimination.set(x, bag);
                        } else {
                            certificateOfElimination.get(x).add(team);
                        }
                    }
                }
            }
        }
    }

    // number of teams
    public int numberOfTeams() {
        return numOfTeams;
    }

    // all teams
    public Iterable<String> teams() {
        Bag<String> teams = new Bag<>();
        for (String str : teamNames) {
            teams.add(str);
        }
        return teams;
    }

    // number of wins for given team
    public int wins(String team) {
        validateTeam(team);
        int index = indexOf(team);
        return w[index];
    }

    // number of losses for given team
    public int losses(String team) {
        validateTeam(team);
        int index = indexOf(team);
        return lose[index];
    }

    // number of remaining games for given team
    public int remaining(String team) {
        validateTeam(team);
        int index = indexOf(team);
        return r[index];
    }

    // number of remaining games between team1 and team2
    public int against(String team1, String team2) {
        validateTeam(team1);
        validateTeam(team2);
        int i = indexOf(team1);
        int j = indexOf(team2);
        return g[i][j];
    }

    // is given team eliminated?
    public boolean isEliminated(String team) {
        validateTeam(team);
        return isEliminated[indexOf(team)];
    }

    // subset R of teams that eliminates given team; null if not eliminated
    public Iterable<String> certificateOfElimination(String team) {
        validateTeam(team);
        return certificateOfElimination.get(indexOf(team));
    }

    // helper functions

    private int calMaxWin() {
        int max = 0;
        for (int i = 0; i < numOfTeams; i++) {
            if (w[max] < w[i])
                max = i;
        }
        return max;
    }

    // construct flow network
    private FlowNetwork constructFN(int x) {
        FlowNetwork fn = new FlowNetwork(this.numOfV);
        // get team index
        int s = numOfV - 2; // source index
        int t = numOfV - 1; // destination index
        // add edges
        // 1. add edges from team vertices to t
        for (int i = 0; i < numOfTeams; i++) {
            if (i == x)
                continue; // no edge connect x
            double capacity = w[x] + r[x] - w[i];
            FlowEdge fe = new FlowEdge(i, t, capacity);
            fn.addEdge(fe);
        }

        // 2. add edges from game vertices to team vertices
        int gameIndex = numOfTeams;
        for (int i = 0; i < numOfTeams - 1; i++) {
            if (i == x)
                continue;
            for (int j = i + 1; j < numOfTeams; j++) {
                if (j == x)
                    continue;
                double capacity = Double.POSITIVE_INFINITY;
                FlowEdge ei = new FlowEdge(gameIndex, i, capacity);
                FlowEdge ej = new FlowEdge(gameIndex, j, capacity);
                fn.addEdge(ei);
                fn.addEdge(ej);

                // 3. add edges from source to game vertices
                FlowEdge eij = new FlowEdge(s, gameIndex, g[i][j]);
                fn.addEdge(eij);

                gameIndex++;
            }
        }
        return fn;
    }

    private void validateTeam(String team) {
        for (String str : teamNames) {
            if (str.equals(team))
                return;
        }
        throw new IllegalArgumentException("invalid team name");
    }

    private int indexOf(String team) {
        for (int i = 0; i < numOfTeams; i++) {
            if (teamNames[i].equals(team))
                return i;
        }
        return -1;
    }

    // unit tests
    public static void main(String[] args) {
        BaseballElimination be = new BaseballElimination(args[0]);
        System.out.println("Eliminated teams: ");
        for (String team : be.teamNames) {
            if (be.isEliminated(team))
                System.out.println(team);
        }
    }

}
