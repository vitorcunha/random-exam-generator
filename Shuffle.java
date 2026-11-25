import java.io.InputStreamReader;
import java.io.FileWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.security.NoSuchAlgorithmException;

import com.google.gson.*;

class Shuffle {

    private static SecureRandom randGenerator;

    /**
     * Return a uniformly distributed integer in the inclusive range [0, max].
     */
    static int rand(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("max must be >= 0");
        }
        if (max == 0) {
            return 0;
        }
        return randGenerator.nextInt(max + 1);
    }

    static void logResponse(FileWriter file, int response) {
        char[] letters = {'a', 'b', 'c', 'd'};

        if (file != null) {
            try {
                file.append(letters[response]);
            } catch (Exception e) {
                System.err.println("Problem writing response: " + e);
            }
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        JsonStreamParser in =
            new JsonStreamParser(new InputStreamReader(System.in));

        ArrayList<JsonObject> questions = new ArrayList<JsonObject>();
        int nrQuestions = 0;
        HashSet<String> dejaVu = new HashSet<String>();
        FileWriter responsesFile = null;

        if (args.length != 0) {
            nrQuestions = Integer.valueOf(args[0]);
        }

        if (args.length >= 2) {
            try {
                responsesFile = new FileWriter(args[1], true);
            } catch (Exception e) {
                System.err.println("Problem opening responses file: " + e);
                System.exit(-1);
            }
        }

        randGenerator = SecureRandom.getInstanceStrong();

        // Read all questions into memory.
        while (in.hasNext()) {
            questions.add(in.next().getAsJsonObject());
        }

        if (nrQuestions == 0) {
            System.out.println("File has " + questions.size() + " questions");
            if (responsesFile != null) {
                try {
                    responsesFile.close();
                } catch (Exception e) {
                    // Nothing useful to do here.
                }
            }
            return;
        }

        // Select and process questions in random order.
        for (; questions.size() > 0 && nrQuestions != 0; nrQuestions--) {
            JsonObject q = questions.remove(rand(questions.size() - 1));
            String type = q.get("type").getAsString();

            // Avoid selecting more than one question with the same non-empty tag.
            if (q.get("tag") != null) {
                String tag = q.get("tag").getAsString();
                if (!tag.equals("")) {
                    if (dejaVu.contains(tag)) {
                        nrQuestions++;
                        continue;
                    }
                    dejaVu.add(tag);
                }
            }

            if (type.equals("1onN")) {
                ArrayList<String> wrongAnswers = new ArrayList<String>();
                ArrayList<String> correctAnswers = new ArrayList<String>();
                JsonArray trueIdxs = q.get("correct").getAsJsonArray();

                // Separate correct and incorrect answers.
                for (int i = 1; ; i++) {
                    if (!q.has(String.valueOf(i))) {
                        break;
                    }

                    boolean isCorrect = false;
                    for (int j = 0; j < trueIdxs.size(); j++) {
                        if (trueIdxs.get(j).getAsInt() == i) {
                            correctAnswers.add(
                                q.get(String.valueOf(i)).getAsString()
                            );
                            isCorrect = true;
                            break;
                        }
                    }

                    if (!isCorrect) {
                        wrongAnswers.add(
                            q.get(String.valueOf(i)).getAsString()
                        );
                    }
                }

                int choices = q.get("choices").getAsInt();

                // This generator supports one correct answer among N choices.
                if (choices < 1) {
                    throw new IllegalArgumentException(
                        "Question choices must be at least 1"
                    );
                }
                if (correctAnswers.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Question has no correct answers: " +
                        q.get("question").getAsString()
                    );
                }
                if (wrongAnswers.size() < choices - 1) {
                    throw new IllegalArgumentException(
                        "Question does not have enough wrong answers: " +
                        q.get("question").getAsString()
                    );
                }

                System.out.println(
                    "\\question\n" + q.get("question").getAsString() +
                    "\n\\begin{parts}"
                );

                /*
                 * Choose the correct answer's position once, uniformly.
                 * The previous sequential-attempt algorithm made the final
                 * position much more likely (42.19% for four choices).
                 */
                int correctPosition = rand(choices - 1);

                for (int i = 0; i < choices; i++) {
                    System.out.print("\\part ");

                    if (i == correctPosition) {
                        System.out.println(
                            correctAnswers.get(rand(correctAnswers.size() - 1))
                        );
                        logResponse(responsesFile, i);
                    } else {
                        System.out.println(
                            wrongAnswers.remove(rand(wrongAnswers.size() - 1))
                        );
                    }
                }

                System.out.println("\\end{parts}");
            }
        }

        if (responsesFile != null) {
            try {
                responsesFile.flush();
                responsesFile.close();
            } catch (Exception e) {
                System.err.println("Problem closing responses file: " + e);
            }
        }
    }
}
