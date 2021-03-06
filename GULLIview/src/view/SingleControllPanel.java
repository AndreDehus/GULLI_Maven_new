package view;

import control.Action.Action;
import control.Controller;
import control.LoadingCoordinator;
import control.StartParameters;
import control.listener.LoadingActionListener;
import control.particlecontrol.ParticlePipeComputing;
import control.particlecontrol.ParticleSurfaceComputing;
import control.particlecontrol.ParticleSurfaceComputing2D;
import control.scenario.Scenario;
import control.scenario.injection.InjectionInformation;
import control.threads.ThreadController;
import io.extran.HE_Database;
import io.extran.Raingauge_Firebird;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import model.particle.Material;
import model.surface.Surface;
import model.surface.measurement.SurfaceMeasurementRaster;
import model.timeline.array.ArrayTimeLineMeasurementContainer;
import model.topology.Network;
import org.jfree.data.time.TimeSeries;
import view.themelayer.PipeThemeLayer;
import view.timeline.PrecipitationTimelinePanel;
import view.timeline.SeriesKey;
import view.timeline.TimeSeriesEditorTablePanel;
import view.video.GIFVideoCreator;

/**
 * This Panel controls one single Simulation and displays information about the
 * simulation time.
 *
 * @author saemann
 */
public class SingleControllPanel extends JPanel implements LoadingActionListener {

    private MapViewer mapViewer;
    private PaintManager paintManager;
    private final ThreadController controler;
    private final Controller control;
    private final BoxLayout layout;
    private JToggleButton buttonRun, buttonStep, buttonReset, buttonPause;
    private JTabbedPane tabs;
    private JPanel panelTabLoading, panelTabSimulation;
    private JTextField textTimeStep;
    private JLabel labelCalculationTime, labelSimulationTime, labelCalculationPerStep, labelCalculationSteps;
    private JPanel panelButtons, panelVideo;//
    private JPanel panelLoading;
    private JPanel panelLoadingStatus, panelLoadingStatusStart, panelLoadingStatusStop;
    private JPanel panelTimeSlide;
    private JLabel labelStarttime, labelEndtime, labelactualTime;
    private JLabel labelParticleActive, labelParticlesTotal;
    private JProgressBar sliderTimeManual;
    private JProgressBar progressLoading;
    private boolean longerThan1Day = false;
    private JCheckBox checkVelocityFunction;
    private JTextField textDispersionPipe, textDispersionSurface;
    private JFormattedTextField textSeed;
    private JButton buttonFileNetwork, buttonFilePipeResult;
    private JButton buttonStartLoading, buttonStartReloadingAll, buttonCancelLoading;
    private JButton buttonFileSurface, buttonFileWaterdepths;

    private boolean wasrunning = false;
    private final JCheckBox checkUpdateIntervall;
    private JPanel panelShapes, panelShapesSurface, panelShapePipe;
    private JSlider sliderTimeShape;
    private JLabel labelSliderTime;

    private JButton newInjectionButton;
    private JButton buttonShowRaingauge;
    private JButton buttonLoadAllPipeTimelines;

    private JPanel panelInjections;

    private ImageIcon iconError, iconLoading, iconPending;

    private JPanel panelMeasurement;
    private JFormattedTextField textMeasurementSecondsPipe;
    private JCheckBox checkMeasureContinouslyPipe;
    private JCheckBox checkMeasureResidenceTimePipe;
    private JFormattedTextField textMeasurementSecondsSurface;
    private JCheckBox checkMeasureContinouslySurface;
//    private JCheckBox checkMeasureResidenceTimeSurface;

    private JButton buttonFileStreetinlets;
    private JLabel labelCurrentAction;

    JRadioButton radioVelocityGDB, radioVelocityWL;
    ButtonGroup groupVelocity;

    protected Action currentAction;

    protected final String updatethreadBarrier = new String("UPDATETHREADBARRIERSINGLECONTROLPANEL");
    protected long updateThreadUpdateIntervalMS = 1000;
    protected Thread updateThread;

    protected PipeThemeLayer activePipeThemeLayer;

    public SingleControllPanel(final ThreadController controller, final Controller control, final JFrame frame, PaintManager pm) {
        super();
        layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        this.setLayout(layout);
        initLoadingIcons();
        this.paintManager = pm;
        this.mapViewer = pm.getMapViewer();
        tabs = new JTabbedPane();
        panelTabLoading = new JPanel(new BorderLayout());
        BoxLayout layoutLoading = new BoxLayout(panelTabLoading, BoxLayout.Y_AXIS);
        panelTabLoading.setLayout(layoutLoading);
        tabs.add("Input", panelTabLoading);
        panelTabSimulation = new JPanel(new BorderLayout());
        BoxLayout layoutSimulation = new BoxLayout(panelTabSimulation, BoxLayout.Y_AXIS);
        panelTabSimulation.setLayout(layoutSimulation);
        tabs.add("Simulation", panelTabSimulation);

        //new BorderLayout());
        this.controler = controller;
        this.control = control;
        this.control.addActioListener(this);
        this.control.getLoadingCoordinator().addActioListener(this);
        this.buttonRun = new JToggleButton(">");
        this.buttonRun.setToolTipText("Start Simulation");
        if (control.getNetwork() == null) {
            buttonRun.setEnabled(false);
        }
        if (buttonRun.getFont().canDisplay('\u25ba')) {
            buttonRun.setText("\u25ba");
        } else if (buttonRun.getFont().canDisplay('\u25f5')) {
            buttonRun.setText("\u25f5");
        }
        this.buttonStep = new JToggleButton(">|");
        this.buttonStep.setToolTipText("1 Step");
        if (buttonStep.getFont().canDisplay('\u23ef')) {
            buttonStep.setText("\u23ef");
        }

        this.buttonPause = new JToggleButton("||");
        this.buttonPause.setToolTipText("Pause");
        if (buttonPause.getFont().canDisplay('\u23f8')) {
            buttonPause.setText("\u23f8");
        }
        this.buttonReset = new JToggleButton("|<<");
        this.buttonReset.setToolTipText("Reset");
        if (buttonReset.getFont().canDisplay('\u23ee')) {
            buttonReset.setText("\u23ee");
        }

        progressLoading = new JProgressBar();

        //Video
        this.panelVideo = new PanelVideoCapture(new GIFVideoCreator(mapViewer));
        this.panelVideo.setBorder(new TitledBorder("Video GIF"));

        //Simulation Control buttons        
        panelButtons = new JPanel(new GridLayout(2, 2));
        panelButtons.setBorder(new TitledBorder("Control"));

        panelButtons.add(buttonRun, 0);
        panelButtons.add(buttonStep, 1);
        buttonStep.setEnabled(false);

        panelButtons.add(buttonPause, 2);
        panelButtons.add(buttonReset, 3);
        this.add(panelButtons);

        this.add(tabs);

        //Loading buttons
        buildFilesLoadingPanel();
        panelTabLoading.add(panelLoading);

        //InjectionInformation
        panelInjections = new JPanel(new BorderLayout());

        JScrollPane scrollinjections = new JScrollPane(panelInjections);
        panelInjections.setBorder(new TitledBorder("Injections"));

        //Add Injection via button
        newInjectionButton = new JButton("New Injection");
        panelInjections.add(newInjectionButton);
        newInjectionButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                InjectionInformation ininfo = new InjectionInformation(0, 1, 1000, new Material("neu", 1000, true), 0, 1);
                control.getLoadingCoordinator().addManualInjection(ininfo);
                control.recalculateInjections();
                SingleControllPanel.this.updateGUI();
            }
        });

//        scrollinjections.setMaximumSize(new Dimension(100, 200));
        panelTabLoading.add(scrollinjections);
        panelTabLoading.add(newInjectionButton);
        // SImulation Parameter
        JPanel panelParameter = new JPanel(new GridLayout(4, 1));
        panelParameter.setBorder(new TitledBorder("Parameter"));
        panelTabSimulation.add(panelParameter);
        //timestep
        JPanel panelParameterTimestep = new JPanel(new BorderLayout());
        panelParameterTimestep.add(new JLabel("Timestep \u0394t :  "), BorderLayout.WEST);
        textTimeStep = new JTextField(controler.getDeltaTime() + "");
        panelParameterTimestep.add(textTimeStep, BorderLayout.CENTER);
        panelParameterTimestep.add(new JLabel("sec."), BorderLayout.EAST);
        panelParameter.add(panelParameterTimestep);
        // Velocity Function instead of Dispersion

        checkVelocityFunction = new JCheckBox("Velocity function", ParticlePipeComputing.useStreamlineVelocity);
        checkVelocityFunction.setToolTipText("Use Streamline equivalent velocity instead of turbulent Dispersion.");
//        panelParameter.add(checkVelocityFunction);
        //Dispersion Pipe
        JPanel panelDispersion = new JPanel(new BorderLayout());
        panelDispersion.add(new JLabel("Pipe Disprs. D : "), BorderLayout.WEST);
        textDispersionPipe = new JTextField(ParticlePipeComputing.getDispersionCoefficient() + "");
        panelDispersion.add(textDispersionPipe, BorderLayout.CENTER);
        panelDispersion.add(new JLabel("m²/s"), BorderLayout.EAST);
        panelParameter.add(panelDispersion);
        //Dispersion Surface
        JPanel panelDispersionSurface = new JPanel(new BorderLayout());
        double d = -1;
        panelDispersionSurface.add(new JLabel("Surface Disp. D : "), BorderLayout.WEST);
        try {
            ParticleSurfaceComputing sc = controller.getParticleThreads()[0].getSurfaceComputing();
            ParticleSurfaceComputing2D sc2d = (ParticleSurfaceComputing2D) sc;
            d = sc2d.getDiffusionCalculator().directD[0];
        } catch (Exception e) {
        }
        textDispersionSurface = new JTextField(d + "");
        panelDispersionSurface.add(textDispersionSurface, BorderLayout.CENTER);
        panelDispersionSurface.add(new JLabel("m²/s"), BorderLayout.EAST);
        panelParameter.add(panelDispersionSurface);
        //Seed
        JPanel panelSeed = new JPanel(new BorderLayout());
        panelSeed.add(new JLabel("Seed :"), BorderLayout.WEST);
        textSeed = new JFormattedTextField(DecimalFormat.getIntegerInstance(StartParameters.formatLocale));
        textSeed.setValue(controller.getSeed());
        panelSeed.add(textSeed, BorderLayout.CENTER);
        panelParameter.add(panelSeed);

        //Panel Measurement/Sampling options
        panelMeasurement = new JPanel(new GridLayout(2, 1));
        panelMeasurement.setBorder(new TitledBorder("Measurements / Sampling"));
        JPanel panelMeasurementsPipe = new JPanel(new GridLayout(2, 1));
        panelMeasurementsPipe.setBorder(new TitledBorder("Pipe Network"));
        JPanel panelMsec = new JPanel(new BorderLayout());

        panelMsec.add(new JLabel("Measure interval: "), BorderLayout.WEST);
        panelMsec.add(new JLabel("sec."), BorderLayout.EAST);
        textMeasurementSecondsPipe = new JFormattedTextField(DecimalFormat.getNumberInstance(StartParameters.formatLocale));
        textMeasurementSecondsPipe.setToolTipText("Length of measurement interval in seconds.");

        panelMsec.add(textMeasurementSecondsPipe, BorderLayout.CENTER);
        panelMeasurementsPipe.add(panelMsec, BorderLayout.NORTH);
        JPanel panelMcheck = new JPanel(new GridLayout(1, 2));
        checkMeasureContinouslyPipe = new JCheckBox("Time contin.", false);
        checkMeasureContinouslyPipe.setToolTipText("<html><b>true</b>: slow, accurate measurement in every simulation timestep, mean calculated for the interval. <br><b>false</b>: fast sampling only at the end of an interval.</html>");

        checkMeasureResidenceTimePipe = new JCheckBox("Space contin.", false);
        checkMeasureResidenceTimePipe.setToolTipText("<html><b>true</b>: Sample all visited capacities. <br><b>false</b>: Sample Only in final capacity at end of simulation step</html>");

        panelMcheck.add(checkMeasureContinouslyPipe);
        panelMcheck.add(checkMeasureResidenceTimePipe);
        panelMeasurementsPipe.add(panelMcheck, BorderLayout.SOUTH);
        panelMeasurement.add(panelMeasurementsPipe);
        panelTabSimulation.add(panelMeasurement);
        if (control != null && control.getScenario() != null) {
            if (control.getScenario().getMeasurementsPipe() != null) {
                ArrayTimeLineMeasurementContainer mpc = control.getScenario().getMeasurementsPipe();
                if (mpc.isTimespotmeasurement()) {
                    checkMeasureContinouslyPipe.setSelected(false);
                } else {
                    checkMeasureContinouslyPipe.setSelected(true);
                }
                checkMeasureResidenceTimePipe.setSelected(!ParticlePipeComputing.measureOnlyFinalCapacity);
                textMeasurementSecondsPipe.setValue(mpc.getDeltaTimeS());
            }
        }
        JPanel panelMeasurementsSurface = new JPanel(new GridLayout(2, 1));
        panelMeasurementsSurface.setBorder(new TitledBorder("Surface"));
        JPanel panelMsecS = new JPanel(new BorderLayout());

        panelMsecS.add(new JLabel("Measure interval: "), BorderLayout.WEST);
        panelMsecS.add(new JLabel("sec."), BorderLayout.EAST);
        textMeasurementSecondsSurface = new JFormattedTextField(DecimalFormat.getNumberInstance(StartParameters.formatLocale));
        textMeasurementSecondsSurface.setToolTipText("Length of measurement interval in seconds.");

        panelMsecS.add(textMeasurementSecondsSurface, BorderLayout.CENTER);
        panelMeasurementsSurface.add(panelMsecS, BorderLayout.NORTH);
        JPanel panelMcheckSurface = new JPanel(new GridLayout(1, 1));
        checkMeasureContinouslySurface = new JCheckBox("Time continous", false);
        checkMeasureContinouslySurface.setToolTipText("<html><b>true</b>: slow, accurate measurement in every simulation timestep, mean calculated for the interval. <br><b>false</b>: fast sampling only at the end of an interval.</html>");

//        checkMeasureResidenceTimeSurface = new JCheckBox("Residence", false);
//        checkMeasureResidenceTimeSurface.setToolTipText("<html><b>true</b>: Sample all visited capacities. <br><b>false</b>: Sample Only in final capacity at end of simulation step</html>");
        panelMcheckSurface.add(checkMeasureContinouslySurface);
//        panelMcheckSurface.add(checkMeasureResidenceTimeSurface);
        panelMeasurementsSurface.add(panelMcheckSurface, BorderLayout.SOUTH);
        panelMeasurement.add(panelMeasurementsSurface);
        if (control.getScenario() != null) {
            if (control.getScenario().getMeasurementsSurface() != null) {
                SurfaceMeasurementRaster mpc = control.getScenario().getMeasurementsSurface();
                if (mpc.continousMeasurements) {
                    checkMeasureContinouslySurface.setSelected(true);
                } else {
                    checkMeasureContinouslySurface.setSelected(false);
                }
                textMeasurementSecondsSurface.setValue(mpc.getIndexContainer().getDeltaTimeMS() / 1000.);
            }
        }

        //Panel Timeline calculation
//        comboTimelineCalculation = new JComboBox<>(ArrayTimeLinePipeContainer.CALCULATION.values());
        JPanel paneltimeLineCalculation = new JPanel(new BorderLayout());
        paneltimeLineCalculation.setBorder(new TitledBorder("Timeline values"));
        paneltimeLineCalculation.add(new JLabel("Calculation Method "), BorderLayout.CENTER);

        ////////////////////
        // Paneltimeslide
        this.panelTimeSlide = new JPanel(new GridLayout(3, 1));
        this.panelTimeSlide.setBorder(new TitledBorder("Simulation time"));
        this.labelStarttime = new JLabel("Start");
        this.labelEndtime = new JLabel("End");
        this.labelEndtime.setHorizontalAlignment(SwingConstants.RIGHT);
        this.labelactualTime = new JLabel("slider");
        this.labelactualTime.setHorizontalAlignment(SwingConstants.CENTER);
        this.sliderTimeManual = new JProgressBar();
        this.sliderTimeManual.setMaximum(1000);
        this.sliderTimeManual.setMinimum(0);
        this.sliderTimeManual.setValue(0);
        JPanel panelTimes = new JPanel(new GridLayout(1, 3));
//        this.sliderTimeManual.setEnabled(false);
        this.sliderTimeManual.setStringPainted(true);
        this.labelSimulationTime = new JLabel();
        this.panelTimeSlide.add(sliderTimeManual, BorderLayout.NORTH);
        panelTimes.add(labelStarttime, BorderLayout.WEST);
        panelTimes.add(labelactualTime, BorderLayout.CENTER);
        panelTimes.add(labelEndtime, BorderLayout.EAST);

        this.panelTimeSlide.add(panelTimes);
//        labelCalculationTime.setHorizontalAlignment(JLabel.CENTER);
        this.panelTimeSlide.add(labelSimulationTime, BorderLayout.SOUTH);

        this.panelTabSimulation.add(panelTimeSlide);

        // Panel Calculation status
        JPanel panelCalculation = new JPanel(new GridLayout(4, 1));
        panelCalculation.setBorder(new TitledBorder("Calculation"));
        JPanel panelCalculationTime = new JPanel(new BorderLayout());
        panelCalculationTime.add(new JLabel("used calc. "), BorderLayout.WEST);
        this.labelCalculationTime = new JLabel("0");
        panelCalculationTime.add(labelCalculationTime, BorderLayout.CENTER);
        labelCalculationTime.setHorizontalAlignment(JLabel.CENTER);
        panelCalculationTime.add(new JLabel("    sec."), BorderLayout.EAST);
        panelCalculation.add(panelCalculationTime);

        JPanel panelCalculationPerStep = new JPanel(new BorderLayout());
        panelCalculationPerStep.add(new JLabel("avrg. need "), BorderLayout.WEST);
        this.labelCalculationPerStep = new JLabel("0");
        panelCalculationPerStep.add(labelCalculationPerStep, BorderLayout.CENTER);
        labelCalculationPerStep.setHorizontalAlignment(JLabel.CENTER);
        panelCalculationPerStep.add(new JLabel(" ms/step"), BorderLayout.EAST);
        panelCalculation.add(panelCalculationPerStep);

        JPanel panelCalculationSteps = new JPanel(new BorderLayout());
        panelCalculationSteps.add(new JLabel("steps : "), BorderLayout.WEST);
        this.labelCalculationSteps = new JLabel("0");
        panelCalculationSteps.add(labelCalculationSteps, BorderLayout.CENTER);
        labelCalculationSteps.setHorizontalAlignment(JLabel.CENTER);
        panelCalculation.add(panelCalculationSteps);

        JPanel panelCalculationParticle = new JPanel(new BorderLayout());
        panelCalculationParticle.add(new JLabel("Particles :"), BorderLayout.WEST);
        this.labelParticleActive = new JLabel("0");
        panelCalculationParticle.add(labelParticleActive, BorderLayout.CENTER);
        labelParticleActive.setHorizontalAlignment(JLabel.CENTER);
        this.labelParticlesTotal = new JLabel("/0");
        panelCalculationParticle.add(labelParticlesTotal, BorderLayout.EAST);
        panelCalculation.add(panelCalculationParticle);

        panelTabSimulation.add(panelCalculation);

        /////////////////////
        ///// ACTION Listener
        buttonFileNetwork.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                String folder = "";
                if (control.getLoadingCoordinator().getFileNetwork() != null) {
                    folder = control.getLoadingCoordinator().getFileNetwork().getAbsolutePath();
                }
                JFileChooser fc = new JFileChooser(folder);
                int n = fc.showOpenDialog(SingleControllPanel.this);
                if (n == JFileChooser.APPROVE_OPTION) {
                    control.getLoadingCoordinator().setPipeNetworkFile(fc.getSelectedFile());
                }
                updateGUI();
            }
        });

        buttonFilePipeResult.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                String folder = "";
                if (control.getLoadingCoordinator().getFileNetwork() != null) {
                    folder = control.getLoadingCoordinator().getFileNetwork().getAbsolutePath();
                }
                JFileChooser fc = new JFileChooser(folder);
                fc.setFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        }
                        return file.getName().toLowerCase().endsWith(".idbf") || file.getName().toLowerCase().endsWith(".idbr");
                    }

                    @Override
                    public String getDescription() {
                        return ".idbf/.idbr HESQL";
                    }
                });
                int n = fc.showOpenDialog(SingleControllPanel.this);
                if (n == fc.APPROVE_OPTION) {
                    //Does Network fit to this result?
                    //Find corresponding Network model file
                    try {
                        LoadingCoordinator.FileContainer c = control.getLoadingCoordinator().findDependentFiles(fc.getSelectedFile(), true);

                        //Find corresponding waterlevel file
                        String question = "Load corresponding files?";
                        if (c.getPipeNetwork() != null) {
                            question += "\n" + c.getPipeNetwork().getParentFile().getName() + "/" + c.getPipeNetwork().getName();
                            if (c.isPipeNetworkLoaded()) {
                                question += " LOADED";
                            }
                        }
                        if (c.getSurfaceDirectory() != null) {
                            question += "\n" + c.getSurfaceDirectory().getParentFile().getName();
                            if (c.isSurfaceTopologyLoaded()) {
                                question += " LOADED";
                            }
                        }
                        if (c.getSurfaceResult() != null) {
                            question += "\n" + c.getSurfaceResult().getParentFile().getName() + "/" + c.getSurfaceResult().getName();
                            if (c.isSurfaceResultLoaded()) {
                                question += " LOADED";
                            }
                        }
                        int m = JOptionPane.showConfirmDialog(SingleControllPanel.this, question, "Dependencies found", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                        if (m == JOptionPane.YES_OPTION || m == JOptionPane.NO_OPTION) {
                            control.getLoadingCoordinator().setPipeResultsFile(fc.getSelectedFile(), true);
                        }

                        if (m == JOptionPane.YES_OPTION) {
                            if (!c.isPipeNetworkLoaded() && c.getPipeNetwork() != null) {
                                control.getLoadingCoordinator().setPipeNetworkFile(c.getPipeNetwork());
                            }
                            if (!c.isSurfaceTopologyLoaded() && c.getSurfaceDirectory() != null) {
                                control.getLoadingCoordinator().setSurfaceTopologyDirectory(c.getSurfaceDirectory());
                            }
                            if (!c.isSurfaceResultLoaded() && c.getSurfaceResult() != null) {
                                control.getLoadingCoordinator().setSurfaceWaterlevelFile(c.getSurfaceResult());
                            }
                        }
                        updateGUI();
                    } catch (SQLException ex) {
                        Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        buttonFileSurface.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                String folder = ".";
                if (control.getSurface() != null && control.getSurface().fileTriangles != null) {
                    folder = control.getSurface().fileTriangles.getAbsolutePath();
                }
                final JFileChooser fc = new JFileChooser(folder) {
                    @Override
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        }
                        if (file.getName().endsWith(".dat")) {
                            return true;
                        }
                        return false;
                    }
                };

                int n = fc.showOpenDialog(SingleControllPanel.this);
                if (n == fc.APPROVE_OPTION) {
                    new Thread("Select File Surface geometry") {

                        @Override
                        public void run() {
                            try {
                                control.getLoadingCoordinator().setSurfaceTopologyDirectory(fc.getCurrentDirectory());
                                buttonFileSurface.setToolTipText("Triangles: " + fc.getSelectedFile().getAbsolutePath());
                                updateGUI();
                            } catch (IOException ex) {
                                Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                    }.start();

                }
            }
        });
        initFileButtonPopupMenu(buttonFileSurface);

        buttonFileStreetinlets.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                String folder = ".";
                final JFileChooser fc = new JFileChooser(folder) {
                    @Override
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        }
                        if (file.getName().endsWith(".shp")) {
                            return true;
                        }
                        return false;
                    }
                };
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

                int n = fc.showOpenDialog(SingleControllPanel.this);
                if (n == fc.APPROVE_OPTION) {
                    new Thread("Select Streetinlets file") {

                        @Override
                        public void run() {

                            try {
                                control.getLoadingCoordinator().setFileStreetInletsSHP(fc.getSelectedFile());
                                buttonFileStreetinlets.setToolTipText("Street Inlets: " + fc.getSelectedFile().getAbsolutePath());
                                updateGUI();
                            } catch (Exception ex) {
                                Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                    }.start();

                }
            }
        });

        buttonFileWaterdepths.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                String folder = ".";
                if (control.getSurface() != null && control.getSurface().fileWaterlevels != null) {
                    folder = control.getSurface().fileWaterlevels.getAbsolutePath();
                }
                final JFileChooser fc = new JFileChooser(folder) {
                    @Override
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        }
                        if (file.getName().endsWith(".shp")) {
                            return true;
                        }
                        if (file.getName().endsWith(".csv")) {
                            return true;
                        }
                        return false;
                    }
                };
                fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

                int n = fc.showOpenDialog(SingleControllPanel.this);
                if (n == fc.APPROVE_OPTION) {
                    new Thread("Select waterlevel file") {

                        @Override
                        public void run() {
                            if (control.getSurface() == null) {
                                System.err.println("No surface set in controller.");
                                return;
                            }
                            try {
                                control.getLoadingCoordinator().setSurfaceWaterlevelFile(fc.getSelectedFile());
                                buttonFileWaterdepths.setToolTipText("Waterlevel: " + fc.getSelectedFile().getAbsolutePath());
                                updateGUI();
                            } catch (Exception ex) {
                                Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }.start();

                }
            }
        });
        initFileButtonPopupMenu(buttonFileWaterdepths);

        buttonStartLoading.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                control.getLoadingCoordinator().startLoadingRequestedFiles(true);
            }
        });

        buttonStartReloadingAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                control.getLoadingCoordinator().startReloadingAll(true);
            }
        });

        buttonCancelLoading.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                control.getLoadingCoordinator().cancelLoading();
            }
        });

        buttonRun.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                buttonRun.setSelected(true);
                buttonPause.setSelected(false);
                control.start();
            }
        });

        buttonStep.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                controler.step();
                buttonRun.setSelected(false);
                buttonPause.setSelected(true);
            }
        });

        buttonPause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                buttonRun.setSelected(false);
                buttonPause.setSelected(true);
                controler.stop();
            }
        });

        buttonReset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                buttonRun.setSelected(false);
                buttonPause.setSelected(true);
                controler.reset();
                control.resetScenario();
                buttonReset.setSelected(false);
            }
        });

        //Update thread to show information about running simulation. e.g. number of active particles
        new Thread("GUI SimulationInformation Update") {
            GregorianCalendar actual = new GregorianCalendar();
            StringBuilder timeelapsed = new StringBuilder(30);
            boolean juststopped = false;

            @Override
            public void run() {
                while (true) {
                    try {
                        if (!controller.isSimulating() && wasrunning) {
                            juststopped = true;
                        }

                        if (controller.isSimulating() || juststopped) {
                            labelCalculationPerStep.setText(controller.getAverageCalculationTime() + "");
                            labelCalculationTime.setText(controller.getElapsedCalculationTime() / 1000 + "");
                            labelCalculationSteps.setText(controller.getSteps() + "");
                            double seconds = ((controller.getSimulationTime() - controller.getSimulationStartTime()) / 1000L);
                            double minutes = seconds / 60.;
                            double hours = minutes / 60.;
                            timeelapsed.delete(0, timeelapsed.capacity());
                            if (hours > 0) {
                                timeelapsed.append((int) hours).append("h ");
                            }
                            if (minutes > 0) {
                                timeelapsed.append((int) minutes % 60).append("m ");
                            }
                            if (seconds > 0) {
                                if ((int) seconds % 60 < 10) {
                                    timeelapsed.append("0");
                                }
                                timeelapsed.append((int) seconds % 60).append("s ");
                            }
                            sliderTimeManual.setValue((int) (0.5 + sliderTimeManual.getMaximum() * (controller.getSimulationTime() - controller.getSimulationStartTime()) / (double) ((controller.getSimulationTimeEnd() - controller.getSimulationStartTime()))));
                            timeelapsed.append(" = ").append(seconds).append("s");
                            labelSimulationTime.setText(timeelapsed.toString());

                            if (!wasrunning && frame != null) {
                                frame.setTitle("> Run Control");
//                                sliderTimeShape.setEnabled(false);
                                wasrunning = true;
                                buttonRun.setSelected(true);
                                buttonPause.setSelected(false);
                                textTimeStep.setEditable(false);
                                textDispersionPipe.setEditable(false);
                                textDispersionSurface.setEditable(false);
                                textSeed.setEditable(false);
                            }
                            frame.setTitle("> " + (int) (((controller.getSimulationTime() - controller.getSimulationStartTime()) * 100 + 0.5) / (double) ((controller.getSimulationTimeEnd() - controller.getSimulationStartTime()))) + "% Run Control");
                            actual.setTimeInMillis(controller.getSimulationTime());
                            labelactualTime.setText(actual.get(GregorianCalendar.HOUR_OF_DAY) + ":" + (actual.get(GregorianCalendar.MINUTE) < 10 ? "0" : "") + (actual.get(GregorianCalendar.MINUTE)) + " ");

                        }
                        if (juststopped) {
                            if (frame != null) {
                                buttonRun.setSelected(false);
                                buttonPause.setSelected(true);
                                frame.setTitle("|| Stop Control");
                                if (sliderTimeShape != null) {
                                    sliderTimeShape.setEnabled(true);
                                }
//                                control.timelinePanel.removeMarker();
                                wasrunning = false;
                                textTimeStep.setEditable(true);//!controler.isSimulating());
                                textDispersionPipe.setEditable(true);//!controler.isSimulating());
                                textDispersionSurface.setEditable(true);//!controler.isSimulating());
                                textSeed.setEditable(true);

                                actual.setTimeInMillis(controller.getSimulationTime());
                                labelactualTime.setText(actual.get(GregorianCalendar.HOUR_OF_DAY) + ":" + (actual.get(GregorianCalendar.MINUTE) < 10 ? "0" : "") + (actual.get(GregorianCalendar.MINUTE)) + " ");

                                labelParticleActive.setText(controller.getNumberOfActiveParticles() + "");
                                labelParticlesTotal.setText("/ " + controller.getNumberOfTotalParticles());

                            }
                        }
                        labelParticleActive.setText(controller.getNumberOfActiveParticles() + "");
                        labelParticlesTotal.setText("/ " + controller.getNumberOfTotalParticles());
                        juststopped = false;
                        wasrunning = controller.isSimulating();
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        }.start();

        textTimeStep.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent fe) {
                super.focusLost(fe); //To change body of generated methods, choose Tools | Templates.
                try {
                    double dt = Double.parseDouble(textTimeStep.getText());
                    controler.setDeltaTime(dt);
                } catch (NumberFormatException numberFormatException) {
                    textTimeStep.setText(controller.getDeltaTime() + "");
                }
            }

        });
        checkVelocityFunction.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                ParticlePipeComputing.useStreamlineVelocity = checkVelocityFunction.isSelected();
                textDispersionPipe.setEnabled(!checkVelocityFunction.isSelected());
            }
        });

        textDispersionPipe.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent ke) {
                try {
                    int pos = textDispersionPipe.getCaretPosition();
                    double v = Double.parseDouble(textDispersionPipe.getText());
                    ParticlePipeComputing.setDispersionCoefficient(v);
//                    textDispersion.setText(ParticlePipeComputing.dispersionCoefficient+"");
                    textDispersionPipe.setForeground(Color.GREEN.darker());
                    textDispersionPipe.setCaretPosition(pos);
                    if (ke.getKeyCode() == 10) {
                        //Confirm by RETURN                        
                        control.setDispersionCoefficientPipe(v);
                        textDispersionPipe.setForeground(Color.BLACK);
                    }
                } catch (NumberFormatException numberFormatException) {
                    textDispersionPipe.setForeground(Color.red);
                }
            }
        });
        textDispersionPipe.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                try {
                    double v = Double.parseDouble(textDispersionPipe.getText());
                    control.setDispersionCoefficientPipe(v);
                } catch (NumberFormatException numberFormatException) {
                    textDispersionPipe.setText(ParticlePipeComputing.getDispersionCoefficient() + "");
                }
                textDispersionPipe.setForeground(Color.BLACK);
            }
        });
        {
            textDispersionSurface.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent ke) {
                    try {
                        int pos = textDispersionSurface.getCaretPosition();
                        double v = Double.parseDouble(textDispersionSurface.getText());
                        textDispersionSurface.setForeground(Color.GREEN.darker());
                        textDispersionSurface.setCaretPosition(pos);
                        if (ke.getKeyCode() == 10) {
                            //Confirm by RETURN                        
                            control.setDispersionCoefficientSurface(v);
                            textDispersionSurface.setForeground(Color.BLACK);
                        }
                    } catch (NumberFormatException numberFormatException) {
                        textDispersionSurface.setForeground(Color.red);
                    }
                }
            });
            textDispersionSurface.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent fe) {
                    try {
                        double v = Double.parseDouble(textDispersionSurface.getText());
                        control.setDispersionCoefficientSurface(v);
                    } catch (NumberFormatException numberFormatException) {
                        textDispersionSurface.setText(ParticlePipeComputing.getDispersionCoefficient() + "");
                    }
                    textDispersionSurface.setForeground(Color.BLACK);
                }
            });
        }
        textSeed.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                controller.setSeed(Long.parseLong(textSeed.getText()));
            }
        });
        textSeed.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent ke) {
                if (ke.getKeyCode() == 10) {
                    //ENTER/RETURN
                    try {
                        long newSeed = Long.parseLong(textSeed.getText());
                        controller.setSeed(newSeed);
                    } catch (Exception exception) {
                        textSeed.setText(controller.getSeed() + "");
                    }
                }
            }

        });
        /////Measurements panel
        checkMeasureContinouslyPipe.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsPipe() != null) {
                    if (checkMeasureContinouslyPipe.isSelected()) {
                        double seconds = ((Number) textMeasurementSecondsPipe.getValue()).doubleValue();

                        control.getScenario().getMeasurementsPipe().setSamplesPerTimeindex(seconds / ThreadController.getDeltaTime());
                    } else {
                        control.getScenario().getMeasurementsPipe().OnlyRecordOncePerTimeindex();
                    }
//                    System.out.println("Sample " + control.getScenario().getMeasurementsPipe().samplesPerTimeinterval + "x per interval");
                }
            }
        });

        checkMeasureResidenceTimePipe.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ParticlePipeComputing.measureOnlyFinalCapacity = !checkMeasureResidenceTimePipe.isSelected();

            }
        });

        textMeasurementSecondsPipe.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                if (textMeasurementSecondsPipe != null || textMeasurementSecondsPipe.getValue() == null) {
                    return;
                }
                double seconds = ((Number) textMeasurementSecondsPipe.getValue()).doubleValue();

                if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsPipe() != null) {
                    if (seconds == control.getScenario().getMeasurementsPipe().getDeltaTimeS()) {
                        return; //DO not change, as the values correspond
                    }
                }
                try {
                    control.getScenario().getMeasurementsPipe().setIntervalSeconds(seconds, control.getScenario().getStartTime(), control.getScenario().getEndTime());
                } catch (Exception e) {
                }
            }
        });
        textMeasurementSecondsPipe.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent ke) {
                if (ke.getKeyCode() == 10) {
                    //ENTER/RETURN
                    try {
                        double seconds = ((Number) textMeasurementSecondsPipe.getValue()).doubleValue();
                        if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsPipe() != null) {
                            if (seconds == control.getScenario().getMeasurementsPipe().getDeltaTimeS()) {
                                return; //DO not change, as the values correspond
                            }
                        }
                        try {
                            control.getScenario().getMeasurementsPipe().setIntervalSeconds(seconds, control.getScenario().getStartTime(), control.getScenario().getEndTime());
                        } catch (Exception e) {
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
//                        textMeasurementSeconds.setValue(control.getScenario().getMeasurementsPipe().getDeltaTimeS());
                    }
                }
            }

        });

        /////Measurements panel
        checkMeasureContinouslySurface.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsSurface() != null) {
                    control.getScenario().getMeasurementsSurface().continousMeasurements = checkMeasureContinouslySurface.isSelected();
                }
            }
        });

        textMeasurementSecondsSurface.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                if (textMeasurementSecondsSurface == null || textMeasurementSecondsSurface.getValue() == null) {
                    return;
                }
                double seconds = Double.parseDouble(textMeasurementSecondsSurface.getText());//((Number) textMeasurementSecondsSurface.getValue()).doubleValue();
                if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsSurface() != null) {
                    if (seconds == control.getScenario().getMeasurementsSurface().getIndexContainer().getDeltaTimeMS() / 1000.) {
                        return;
                    }
                }
                try {
                    control.getScenario().getMeasurementsSurface().setIntervalSeconds(seconds, control.getScenario().getStartTime(), control.getScenario().getEndTime());
                } catch (Exception e) {
                }
            }
        });
        textMeasurementSecondsSurface.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent ke) {
                if (ke.getKeyCode() == 10) {
                    //ENTER/RETURN
                    if (textMeasurementSecondsSurface == null || textMeasurementSecondsSurface.getValue() == null) {
                        return;
                    }
                    double seconds = Double.parseDouble(textMeasurementSecondsSurface.getText());//((Number) textMeasurementSecondsSurface.getValue()).doubleValue();
                    if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsSurface() != null) {
                        if (seconds == control.getScenario().getMeasurementsSurface().getIndexContainer().getDeltaTimeMS() / 1000.) {
                            return;
                        }
                    }
                    try {
                        control.getScenario().getMeasurementsSurface().setIntervalSeconds(seconds, control.getScenario().getStartTime(), control.getScenario().getEndTime());
                    } catch (Exception e) {
                    }
                }
            }

        });

        //////////////////////////////////////////////////////////////////////
        ///// Panel VIEW
        JPanel panelView = new JPanel(new BorderLayout());
        panelView.setBorder(new TitledBorder("Draw Update"));
        checkUpdateIntervall = new JCheckBox("Update View 1/", controller.paintOnMap);
        final JTextField textUpdateLoops = new JTextField(controller.paintingInterval + "");
        checkUpdateIntervall.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                controller.paintOnMap = checkUpdateIntervall.isSelected();
                if (checkUpdateIntervall.isSelected()) {
                    try {
                        int loops = Integer.parseInt(textUpdateLoops.getText());
                        controller.paintingInterval = loops;
                        paintManager.repaintPerLoops = loops;
                    } catch (Exception exception) {
                        textUpdateLoops.setText(controller.paintingInterval + "");
                    }
                } else {
                    paintManager.repaintPerLoops = Integer.MAX_VALUE;
                }
            }
        });
        panelView.setPreferredSize(new Dimension(220, 50));
        panelView.setMaximumSize(new Dimension(400, 50));
        panelView.add(checkUpdateIntervall, BorderLayout.WEST);

        textUpdateLoops.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent ke) {
                if (ke.getKeyCode() == 10) {
                    try {
                        int loops = Integer.parseInt(textUpdateLoops.getText());
                        controller.paintingInterval = loops;
                        paintManager.repaintPerLoops = loops;
                    } catch (Exception exception) {
                        textUpdateLoops.setText(controller.paintingInterval + "");
                    }
                }
            }
        });
        textUpdateLoops.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                try {
                    int loops = Integer.parseInt(textUpdateLoops.getText());
                    controller.paintingInterval = loops;
                    paintManager.repaintPerLoops = loops;
                } catch (Exception exception) {
                    textUpdateLoops.setText(controller.paintingInterval + "");
                }
            }
        });
        panelView.add(textUpdateLoops, BorderLayout.CENTER);
        panelView.add(new JLabel("loops"), BorderLayout.EAST);
        panelTabSimulation.add(panelView);

        ////////////////////////////////////////////////////////////////////////
        ////// Panelshapeview
        panelShapes = new JPanel();
        panelShapes.setBorder(new TitledBorder("Shapeview"));
        panelShapes.setLayout(new BoxLayout(panelShapes, BoxLayout.Y_AXIS));
        sliderTimeShape = new JSlider(0, 1000, 0);
        panelShapes.add(sliderTimeShape);
        labelSliderTime = new JLabel();
        labelSliderTime.setAlignmentX(0.5f);
        panelShapes.add(labelSliderTime);

        this.add(panelShapes);
        panelShapePipe = new JPanel(new GridLayout(4, 1));
        panelShapePipe.setBorder(new TitledBorder("Pipe Shapes"));
        final JComboBox<PaintManager.PIPESHOW> comboPipeShow = new JComboBox<>(PaintManager.PIPESHOW.values());

        final JComboBox<PipeThemeLayer.LAYERS> comboPipeThemes = new JComboBox<>(PipeThemeLayer.LAYERS.values());

        comboPipeShow.setSelectedItem(PaintManager.PIPESHOW.GREY);
        buttonLoadAllPipeTimelines = new JButton("Load all Timelines");
        buttonLoadAllPipeTimelines.setToolTipText("Show values for all pipes & manholes, not only for the subset of affected capacities.");
        panelShapePipe.add(buttonLoadAllPipeTimelines);
        panelShapePipe.add(comboPipeThemes);
        panelShapePipe.add(comboPipeShow);

        panelShapes.add(panelShapePipe);

        panelShapesSurface = new JPanel(new GridLayout(2, 1));
        panelShapesSurface.setBorder(new TitledBorder("Surface Shapes"));
        final JComboBox<PaintManager.SURFACESHOW> comboSurfaceShow = new JComboBox<>(PaintManager.SURFACESHOW.values());
        if (paintManager != null) {
            comboSurfaceShow.setSelectedItem(paintManager.getSurfaceShow());
        }
        final JCheckBox checkTrianglesNodes = new JCheckBox("Triangles as Nodes", paintManager.isDrawingTrianglesAsNodes());
        panelShapesSurface.add(comboSurfaceShow, BorderLayout.NORTH);
        panelShapesSurface.add(checkTrianglesNodes, BorderLayout.SOUTH);
        panelShapes.add(panelShapesSurface);

        final JCheckBox checkPipeArrows = new JCheckBox("Pipes as Arrows", paintManager.isDrawPipesAsArrows());
        panelShapePipe.add(checkPipeArrows, BorderLayout.CENTER);
        comboPipeShow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                paintManager.setPipeShow((PaintManager.PIPESHOW) comboPipeShow.getSelectedItem());
            }
        });
        comboPipeThemes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (activePipeThemeLayer == PipeThemeLayer.LAYERS.values()[comboPipeThemes.getSelectedIndex()].getTheme()) {
                    //Nothing changed.
                    return;
                }
                if (activePipeThemeLayer != null) {
                    activePipeThemeLayer.removeTheme(mapViewer);
                }
                activePipeThemeLayer = PipeThemeLayer.LAYERS.values()[comboPipeThemes.getSelectedIndex()].getTheme();
                activePipeThemeLayer.initializeTheme(mapViewer, control);
                mapViewer.recalculateShapes();
                mapViewer.repaint();
            }
        });
        comboSurfaceShow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                paintManager.setSurfaceShow((PaintManager.SURFACESHOW) comboSurfaceShow.getSelectedItem());
                comboSurfaceShow.setSelectedItem(paintManager.getSurfaceShow());
                mapViewer.recalculateShapes();
                mapViewer.repaint();
            }
        });

        buttonLoadAllPipeTimelines.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    control.getLoadingCoordinator().loadTimelinesOfAllElements();
                } catch (IOException ex) {
                    Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParseException ex) {
                    Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SQLException ex) {
                    Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        sliderTimeShape.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                long time = (long) (controller.getSimulationStartTime() + (controller.getSimulationTimeEnd() - controller.getSimulationStartTime()) * (sliderTimeShape.getValue() / (double) sliderTimeShape.getMaximum()));
                paintManager.setTimeToShow(time);
                if (control.getSingleEventInputData() != null) {
                    control.getSingleEventInputData().getManholeTimeline().setActualTime(time);
                    control.getSingleEventInputData().getPipeTimeline().setActualTime(time);
                }
                if (control.getScenario() != null) {
                    control.getScenario().setActualTime(time);
                }
                if (ArrayTimeLineMeasurementContainer.instance != null) {
                    ArrayTimeLineMeasurementContainer.instance.setActualTime(time);
                }
                double seconds = ((time - controller.getSimulationStartTime()) / 1000L);
                double minutes = seconds / 60.;
                double hours = minutes / 60.;
                String timeelapsed = "";
                if (hours > 0) {
                    timeelapsed += (int) hours + "h ";
                }
                if (minutes > 0) {
                    timeelapsed += (int) minutes % 60 + "m ";
                }
                if (seconds > 0) {
                    timeelapsed += (int) seconds % 60 + "s ";
                }
                labelSliderTime.setText(timeelapsed);
                mapViewer.needMapUpdate();
                mapViewer.repaint();
            }
        });
        checkPipeArrows.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                paintManager.setDrawPipesAsArrows(checkPipeArrows.isSelected());
            }
        });

        checkTrianglesNodes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                paintManager.setSurfaceTrianglesShownAsNodes(checkTrianglesNodes.isSelected());
            }
        });

        // Zusammenbauen
        this.add(panelVideo);

        //Button Raingauge
        buttonShowRaingauge = new JButton("Raingauge");
        buttonShowRaingauge.setToolTipText("Show Raingauge of current scenario.");
        this.add(buttonShowRaingauge);
        buttonShowRaingauge.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                JFrame frame = new JFrame("Raingauge");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setBounds(SingleControllPanel.this.getBounds().x + SingleControllPanel.this.getBounds().width, SingleControllPanel.this.getBounds().y + 30, 400, 300);
                //Timelinepanel
                PrecipitationTimelinePanel timelinePanel = new PrecipitationTimelinePanel("Precipitation", control);
                timelinePanel.startAtZero = true;
                frame.add(timelinePanel, BorderLayout.CENTER);
                File file = control.getLoadingCoordinator().getFilePipeResultIDBF();
                if (file != null) {
                    try {
                        try {
                            frame.setTitle("Raingauge of Piperesults '" + HE_Database.readResultname(file) + "'");
                        } catch (Exception exception) {
                            frame.setTitle("Exception reading file: " + exception.getLocalizedMessage());
                        }
                        Raingauge_Firebird raingauge = HE_Database.readRegenreihe(file);
//                        System.out.println("Raingauge to show: "+raingauge);
                        TimeSeries ts = timelinePanel.createRainGaugeIntervalTimeSeries(raingauge);
                        ((SeriesKey) ts.getKey()).renderAsBar = true;
                        timelinePanel.getCollection().addSeries(ts);

                    } catch (Exception ex) {
                        Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                timelinePanel.showCheckBoxPanel(false);
                TimeSeriesEditorTablePanel panelTable = new TimeSeriesEditorTablePanel(timelinePanel);
                panelTable.setPreferredSize(new Dimension(30, 100));
                frame.add(panelTable, BorderLayout.SOUTH);
                frame.setVisible(true);

                //aktivate visibility of timeseries
//                for (Object series : timelinePanel.getCollection().getSeries()) {
//                    if (series instanceof TimeSeries) {
//                        TimeSeries ts = (TimeSeries) series;
//                        SeriesKey key = (SeriesKey) ts.getKey();
//                        key.visible = true;
//                        System.out.println("KEY" + ts.getKey());
//                    }
//                }
                panelTable.getTable().collectionChanged();
            }
        });

        JPanel panelstretch = new JPanel(new BorderLayout());
        this.add(panelstretch);

        startGUIUpdateThread();
    }

    public void updateGUI() {
        synchronized (updatethreadBarrier) {
            updatethreadBarrier.notifyAll();
        }
    }

    private void initTransferHandlerFile(JPanel panelFile) {
        TransferHandler th = new TransferHandler() {
            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                for (DataFlavor flavor : support.getDataFlavors()) {
                    if (flavor.isFlavorJavaFileListType()) {
                        return true;
                    }
                }
                return false;
            }

            @Override

            public boolean importData(TransferHandler.TransferSupport support) {
                if (!this.canImport(support)) {
                    return false;
                }

                List<File> files;
                try {
                    files = (List<File>) support.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                } catch (UnsupportedFlavorException | IOException ex) {
                    // should never happen (or JDK is buggy)
                    ex.printStackTrace();
                    return false;
                }
                System.out.println("input: " + files.size() + " files.");
                for (File file : files) {
                    System.out.println(file.getAbsolutePath());
                    if (file.getName().endsWith("idbf") || file.getName().endsWith("idbr") || file.getName().endsWith("idbm")) {
                        try {
                            control.getLoadingCoordinator().requestDependentFiles(file, true, true);
//                        control.getLoadingCoordinator().setPipeResultsFile(file, true);
//                        control.loadSingleEventFirebirdDatabase(file, true);
                        } catch (SQLException ex) {
                            Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    } else {
                        System.out.println("wrong suffix : " + file.getAbsolutePath());
                    }
                }
                return true;
            }

        };

        panelFile.setTransferHandler(th);
        panelLoading.setTransferHandler(th);
    }

    private Color getLoadingColor(LoadingCoordinator.LOADINGSTATUS status) {
        if (status == LoadingCoordinator.LOADINGSTATUS.ERROR) {
            return new Color(255, 150, 150);
        }
        if (status == LoadingCoordinator.LOADINGSTATUS.LOADING) {
            return new Color(200, 230, 255);
        }
        if (status == LoadingCoordinator.LOADINGSTATUS.LOADED) {
            return new Color(200, 255, 200);
        }
        if (status == LoadingCoordinator.LOADINGSTATUS.NOT_REQUESTED) {
            return Color.lightGray;
        }
        if (status == LoadingCoordinator.LOADINGSTATUS.REQUESTED) {
            return Color.yellow;
        }

        return Color.magenta;

    }

    private void initLoadingIcons() {
        int size = 12;
        URL res = this.getClass().getResource("/icons/cross_darkred.png");

        iconError = new ImageIcon(new ImageIcon(res).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));

        iconLoading = new ImageIcon(new ImageIcon(this.getClass().getResource("/icons/working_white.png")).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
        iconPending = new ImageIcon(new ImageIcon(this.getClass().getResource("/icons/3dots_black.png")).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));

    }

    private Icon getLoadingIcon(LoadingCoordinator.LOADINGSTATUS status) {
        if (status == LoadingCoordinator.LOADINGSTATUS.ERROR) {
            return iconError;
        }
        if (status == LoadingCoordinator.LOADINGSTATUS.LOADING) {
            return iconLoading;
        }

        if (status == LoadingCoordinator.LOADINGSTATUS.REQUESTED) {
            return iconPending;
        }

        return null;
    }

    private void initFileButtonPopupMenu(final JButton button) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem itemDeactivate = new JMenuItem("Do NOT use");
        itemDeactivate.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (button == buttonFileSurface) {
                    try {
                        control.getLoadingCoordinator().setSurfaceTopologyDirectory(null);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    control.loadSurface(null, SingleControllPanel.this);
                } else if (button == buttonFileWaterdepths) {
                    try {
                        control.getLoadingCoordinator().setSurfaceWaterlevelFile(null);

                    } catch (Exception ex) {
                        Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }

                } else {
                    System.out.println("Button " + button.getText() + " can not be deactivated.");
                }
                updateGUI();
            }
        });
        menu.add(itemDeactivate);
        button.setComponentPopupMenu(menu);
    }

    private JPanel buildFilesLoadingPanel() {
        //Pipenetwork File 
        panelLoading = new JPanel();
        panelLoading.setLayout(new BoxLayout(panelLoading, BoxLayout.Y_AXIS));
        panelLoading.setBorder(new TitledBorder("Files & Loading"));

//        panelFile = new JPanel(new BorderLayout());
//        panelLoading.add(panelFile);
//        panelLoading.setPreferredSize(new Dimension(200, 250));
        this.buttonStartLoading = new JButton("Start Loading");
        this.buttonStartReloadingAll = new JButton("Reload all");
        this.buttonCancelLoading = new JButton("Stop");
        this.buttonCancelLoading.setToolTipText("Interrupts the loading Thread");

        //Buttons to select files
        //Pipe Network 
        JPanel panelNetwork = new JPanel(new GridLayout(2, 1));
        panelNetwork.setBorder(new TitledBorder("Pipe Network"));
        this.buttonFileNetwork = new JButton("Network Topology");
//        panelNetwork.add(new JLabel("Pipe Network "), BorderLayout.WEST);
        panelNetwork.add(buttonFileNetwork);
        panelLoading.add(panelNetwork);

        //Pipe velocities
//        JPanel panelPipeVelocity = new JPanel(new BorderLayout());
        this.buttonFilePipeResult = new JButton("Pipe Velocity");
//        panelPipeVelocity.add(new JLabel("Pipe Velocity "), BorderLayout.WEST);
        panelNetwork.add(buttonFilePipeResult);
//        panelLoading.add(panelPipeVelocity);
//        this.initTransferHandlerFile(panelFile);

        //Surface Panel
        JPanel panelSurface = new JPanel(new GridLayout(3, 1));
        panelSurface.setBorder(new TitledBorder("Surface"));
        JPanel panelStreetInlets = new JPanel(new BorderLayout());
        this.buttonFileStreetinlets = new JButton("StreetInlets Position");
        this.buttonFileStreetinlets.setToolTipText("Shapefile with Point locations of streetinlets. Link between Surface and Pipes are auto created.");
        final JCheckBox checkStreetInlets = new JCheckBox(" Active", true);
        checkStreetInlets.setToolTipText("Switch simulation transport on surface.");
        panelStreetInlets.add(checkStreetInlets, BorderLayout.WEST);
        panelStreetInlets.add(buttonFileStreetinlets, BorderLayout.CENTER);
        panelSurface.add(panelStreetInlets);
        checkStreetInlets.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (checkStreetInlets.isSelected()) {
//                    if (control.getLoadingCoordinator().getFileSurfaceTriangleIndicesDAT() != null && control.getLoadingCoordinator().getLoadingSurface() != LoadingCoordinator.LOADINGSTATUS.LOADED) {
//                        try {
//                            control.getLoadingCoordinator().setSurfaceTopologyDirectory(control.getLoadingCoordinator().getFileSurfaceTriangleIndicesDAT());
//                        } catch (FileNotFoundException ex) {
//                            Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//                    }
//                    if (control.getLoadingCoordinator().getFileSurfaceWaterlevels() != null && control.getLoadingCoordinator().getLoadingSurfaceVelocity() != LoadingCoordinator.LOADINGSTATUS.LOADED) {
//                        control.getLoadingCoordinator().setSurfaceWaterlevelFile(control.getLoadingCoordinator().getFileSurfaceWaterlevels());
//                    }
                    if (control.getSurface() != null) {
                        controler.loadSurface(control.getSurface(), ae);
                    }
                } else {
                    controler.loadSurface(null, SingleControllPanel.this);
//                    control.getLoadingCoordinator().stopLoadingSurfaceGrid();
//                    control.getLoadingCoordinator().stopLoadingSurfaceVelocity();
                }
//                updateGUI();
            }
        });

//        checkStreetInlets.setEnabled(false);
//        panelLoading.add(panelStreetInlets);
        //Surface File
//        JPanel panelSurfaceGrid = new JPanel(new BorderLayout());
        this.buttonFileSurface = new JButton("Surface Grid");
        panelSurface.add(buttonFileSurface);
//        JCheckBox checkSurfaceGrid = new JCheckBox("Surface Grid", true);
//        checkSurfaceGrid.setEnabled(false);
//        panelSurfaceGrid.add(checkSurfaceGrid, BorderLayout.WEST);
//        panelSurfaceGrid.add(buttonFileSurface, BorderLayout.CENTER);
//        panelLoading.add(panelSurfaceGrid);

        //Surface Velocity/Waterlevel
//        JPanel panelSurfaceVelocity = new JPanel(new BorderLayout());
        this.buttonFileWaterdepths = new JButton("Velocity / Waterlevel");
        panelSurface.add(buttonFileWaterdepths);
//        JCheckBox checkSurfaceVelocity = new JCheckBox("Surface Velocity", true);
//        checkSurfaceVelocity.setEnabled(false);
//        panelSurfaceVelocity.add(checkSurfaceVelocity, BorderLayout.WEST);
//        panelSurfaceVelocity.add(buttonFileWaterdepths, BorderLayout.CENTER);
//        panelLoading.add(panelSurfaceVelocity);
        // Row Velocity loading gdb/WL
        radioVelocityGDB = new JRadioButton("GDB", control.getLoadingCoordinator().isLoadGDBVelocity());
        radioVelocityGDB.setToolTipText("Prioritise loading surface velocities directly from GDB file.");
        radioVelocityWL = new JRadioButton("WaterLevel", !control.getLoadingCoordinator().isLoadGDBVelocity());
        radioVelocityWL.setToolTipText("Prioritise calculating of surface velocities from waterlevels.");
        groupVelocity = new ButtonGroup();
        groupVelocity.add(radioVelocityGDB);
        groupVelocity.add(radioVelocityWL);
        JPanel panelRadiobuttons = new JPanel(new BorderLayout());
        panelRadiobuttons.add(radioVelocityWL, BorderLayout.WEST);
        panelRadiobuttons.add(radioVelocityGDB, BorderLayout.EAST);
//        panelSurface.add(panelRadiobuttons);
        radioVelocityGDB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                control.getLoadingCoordinator().setUseGDBVelocity(radioVelocityGDB.isSelected());
            }
        });

        radioVelocityWL.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                control.getLoadingCoordinator().setUseGDBVelocity(!radioVelocityWL.isSelected());
            }
        });
        this.panelLoading.add(panelSurface);
        panelLoadingStatus = new JPanel(new BorderLayout());
        panelLoadingStatus.setPreferredSize(new Dimension(200, 30));
        JPanel panelLoadingActionLabel = new JPanel(new BorderLayout());
        labelCurrentAction = new JLabel("initializing");
        labelCurrentAction.setAlignmentX(0);
        labelCurrentAction.setPreferredSize(new Dimension(150, 20));
        panelLoadingActionLabel.add(labelCurrentAction);
        panelLoading.add(panelLoadingActionLabel);
        panelLoading.add(panelLoadingStatus);

        //Build subpanels that can be inside the status panel.
        panelLoadingStatusStop = new JPanel(new BorderLayout());
        panelLoadingStatusStop.add(progressLoading, BorderLayout.CENTER);
        panelLoadingStatusStop.add(buttonCancelLoading, BorderLayout.EAST);

        panelLoadingStatusStart = new JPanel(new BorderLayout());
        panelLoadingStatusStart.add(buttonStartReloadingAll, BorderLayout.WEST);
        panelLoadingStatusStart.add(buttonStartLoading, BorderLayout.EAST);

        panelLoadingStatus.add(panelLoadingStatusStart);

        return panelLoading;
    }

    public void startUpdateThread() {
        if (control.getLoadingCoordinator().action != null) {
            final Action action = control.getLoadingCoordinator().action;
            if (action.hasProgress) {
                Thread update = new Thread("UpdateProgress_" + action.description) {
                    int loops = 0;
                    int lastprogress = -1;

                    @Override
                    public void run() {
                        while (loops++ < 100) {
                            //Only update if this is the current working 
                            if (action == control.getLoadingCoordinator().action) {
                                if ((int) (action.progress * 100) > lastprogress) {
                                    loops = 0;
                                    lastprogress = (int) (action.progress * 100);
                                    progressLoading.setIndeterminate(false);
                                    progressLoading.setValue(lastprogress);
                                }
                            } else {
                                break;
                            }
                        }
                        System.out.println("Died update thread for " + action.description);
                    }

                };
                update.start();
            } else {
                progressLoading.setIndeterminate(true);
            }
        }
    }

    @Override
    public void actionFired(Action action, Object source) {
        this.currentAction = action;
        updateGUI();
    }

    @Override
    public void loadNetwork(Network network, Object caller) {
        updateGUI();
    }

    @Override
    public void loadSurface(Surface surface, Object caller) {
        updateGUI();
    }

    @Override
    public void loadScenario(Scenario scenario, Object caller) {
        updateGUI();
    }

    private void startGUIUpdateThread() {
        updateThread = new Thread("GUI Repaint SinglecontrolPanel") {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        GregorianCalendar start = new GregorianCalendar();
                        start.setTimeInMillis(controler.getSimulationStartTime());
                        GregorianCalendar end = new GregorianCalendar();
                        end.setTimeInMillis(controler.getSimulationTimeEnd());

                        longerThan1Day = false;
                        if (start.get(GregorianCalendar.DAY_OF_YEAR) != start.get(GregorianCalendar.DAY_OF_YEAR) || start.get(GregorianCalendar.YEAR) != start.get(GregorianCalendar.YEAR)) {
                            longerThan1Day = true;
                        }
                        labelStarttime.setToolTipText(new Date(controler.getSimulationStartTime()).toString());
                        labelEndtime.setToolTipText(new Date(controler.getSimulationTimeEnd()).toString());
                        if (longerThan1Day) {
                            labelStarttime.setText(start.get(GregorianCalendar.DAY_OF_MONTH) + "." + (start.get(GregorianCalendar.MONTH) + 1) + " ");
                            labelEndtime.setText(end.get(GregorianCalendar.DAY_OF_MONTH) + "." + (end.get(GregorianCalendar.MONTH) + 1) + " ");
                        } else {
                            labelStarttime.setText("");
                            labelEndtime.setText("");
                        }
                        labelStarttime.setText(labelStarttime.getText() + start.get(GregorianCalendar.HOUR_OF_DAY) + ":" + (start.get(GregorianCalendar.MINUTE) < 10 ? "0" : "") + (start.get(GregorianCalendar.MINUTE)) + " ");
                        labelEndtime.setText(labelEndtime.getText() + end.get(GregorianCalendar.HOUR_OF_DAY) + ":" + (end.get(GregorianCalendar.MINUTE) < 10 ? "0" : "") + (end.get(GregorianCalendar.MINUTE)) + " ");

                        sliderTimeManual.setValue(0);
                        LoadingCoordinator lc = control.getLoadingCoordinator();

                        buttonFileNetwork.setBackground(getLoadingColor(lc.getLoadingpipeNetwork()));
                        buttonFileNetwork.setIcon(getLoadingIcon(lc.getLoadingpipeNetwork()));
                        if (lc.getFileNetwork() != null) {
                            buttonFileNetwork.setToolTipText(lc.getLoadingSurface() + ": " + lc.getFileNetwork().getAbsolutePath());
                        } else {
                            buttonFileNetwork.setToolTipText("Not set");
                        }

                        buttonFilePipeResult.setBackground(getLoadingColor(lc.getLoadingPipeResult()));
                        buttonFilePipeResult.setIcon(getLoadingIcon(lc.getLoadingPipeResult()));
                        if (lc.getFilePipeResultIDBF() != null) {
                            buttonFilePipeResult.setToolTipText(lc.getLoadingPipeResult() + ": " + lc.getFilePipeResultIDBF().getAbsolutePath());
                            if (lc.getLoadingPipeResult() == LoadingCoordinator.LOADINGSTATUS.LOADED) {
                            }
                        } else {
                            buttonFilePipeResult.setToolTipText("Not set");
                        }

                        buttonFileSurface.setBackground(getLoadingColor(lc.getLoadingSurface()));
                        buttonFileSurface.setIcon(getLoadingIcon(lc.getLoadingSurface()));
                        if (lc.getFileSurfaceTriangleIndicesDAT() != null) {
                            buttonFileSurface.setToolTipText(lc.getLoadingSurface() + ": " + lc.getFileSurfaceTriangleIndicesDAT().getParent());
                        } else {
                            buttonFileSurface.setToolTipText("Not set");
                        }

                        buttonFileStreetinlets.setBackground(getLoadingColor(lc.getLoadingStreetInlets()));
                        buttonFileStreetinlets.setIcon(getLoadingIcon(lc.getLoadingStreetInlets()));
                        if (lc.getFileStreetInletsSHP() != null) {
                            buttonFileStreetinlets.setToolTipText(lc.getLoadingStreetInlets() + ": " + lc.getFileStreetInletsSHP().getAbsolutePath());
                        } else {
                            buttonFileStreetinlets.setToolTipText("Not set");
                        }

                        buttonFileWaterdepths.setBackground(getLoadingColor(lc.getLoadingSurfaceVelocity()));
                        buttonFileWaterdepths.setIcon(getLoadingIcon(lc.getLoadingSurfaceVelocity()));
                        if (lc.getFileSurfaceWaterlevels() != null) {
                            buttonFileWaterdepths.setToolTipText(lc.getLoadingSurfaceVelocity() + ": " + lc.getFileSurfaceWaterlevels().getAbsolutePath());
                        } else {
                            buttonFileWaterdepths.setToolTipText("Not set");
                        }

//                        if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsPipe() != null) {
//                            ArrayTimeLineMeasurementContainer mpc = control.getScenario().getMeasurementsPipe();
//                            if (mpc.isTimespotmeasurement()) {
//                                checkMeasureContinously.setSelected(false);
//                            } else {
//                                checkMeasureContinously.setSelected(true);
//                            }
//                            checkMeasureResidenceTime.setSelected(!ParticlePipeComputing.measureOnlyFinalCapacity);
//                            textMeasurementSeconds.setValue(mpc.getDeltaTimeS());
//                        }
                        if (control != null && control.getScenario() != null) {
                            if (control.getScenario().getMeasurementsPipe() != null) {
                                ArrayTimeLineMeasurementContainer mpc = control.getScenario().getMeasurementsPipe();
                                if (mpc.isTimespotmeasurement()) {
                                    checkMeasureContinouslyPipe.setSelected(false);
                                } else {
                                    checkMeasureContinouslyPipe.setSelected(true);
                                }
                                checkMeasureResidenceTimePipe.setSelected(!ParticlePipeComputing.measureOnlyFinalCapacity);
                                textMeasurementSecondsPipe.setValue(mpc.getDeltaTimeS());
                            }
                            if (control.getScenario().getMeasurementsSurface() != null) {
                                SurfaceMeasurementRaster mpc = control.getScenario().getMeasurementsSurface();
                                if (mpc.continousMeasurements) {
                                    checkMeasureContinouslySurface.setSelected(true);
                                } else {
                                    checkMeasureContinouslySurface.setSelected(false);
                                }
                                textMeasurementSecondsSurface.setValue(mpc.getIndexContainer().getDeltaTimeMS() / 1000.);
                            }
                        }

                        textSeed.setText(control.getThreadController().getSeed() + "");

//                            if (lc.isLoading()) {
//                                if (progressLoading == null) {
//                                    progressLoading = new JProgressBar();
//                                    progressLoading.setBackground(getLoadingColor(LoadingCoordinator.LOADINGSTATUS.LOADING));
//                                }
//
//                                progressLoading.setIndeterminate(true);
//                                panelLoadingStatus.remove(buttonStartLoading);
//                                panelLoadingStatus.remove(buttonStartReloadingAll);
//                                panelLoadingStatus.add(progressLoading, BorderLayout.CENTER);
//                                panelLoadingStatus.add(buttonCancelLoading, BorderLayout.EAST);
//                                if (lc.action != null && !lc.action.description.equals(labelCurrentAction.getText())) {
//                                    labelCurrentAction.setText(lc.action.description);
//                                    startUpdateThread();
//                                }
//                                panelLoadingStatus.revalidate();
//                                panelLoading.revalidate();
//                                panelLoadingStatus.repaint();
//                            } else {
//                                if (progressLoading != null) {
//                                    panelLoadingStatus.remove(progressLoading);
//                                    panelLoadingStatus.remove(buttonCancelLoading);
//                                    panelLoadingStatus.add(buttonStartLoading, BorderLayout.EAST);
//                                    panelLoadingStatus.add(buttonStartReloadingAll, BorderLayout.WEST);
//                                }
//                                labelCurrentAction.setText(currentAction.toString());
//                                panelLoadingStatus.revalidate();
//                                panelLoading.revalidate();
//                                panelLoadingStatus.repaint();
//                            }
                        textTimeStep.setText(ThreadController.getDeltaTime() + "");
                        checkUpdateIntervall.setSelected(controler.paintOnMap);

                        labelParticleActive.setText(controler.getNumberOfActiveParticles() + "");
                        labelParticlesTotal.setText("/ " + controler.getNumberOfTotalParticles());

                        buttonRun.setEnabled((control.getNetwork() != null || control.getSurface() != null) && !control.getLoadingCoordinator().isLoading());

                        textTimeStep.setEditable(!controler.isSimulating());
                        textDispersionPipe.setEditable(!controler.isSimulating());
                        textDispersionSurface.setEditable(!controler.isSimulating());

                        //Information about shapes
                        if (control.getNetwork() != null && control.getNetwork().getPipes() != null) {
                            if (panelShapePipe.getBorder() instanceof TitledBorder) {
                                ((TitledBorder) panelShapePipe.getBorder()).setTitle("Pipe Shapes (" + control.getNetwork().getPipes().size() + ")");
                            }
                        }
                        if (control.getSurface() != null && control.getSurface().getTriangleMids() != null) {
                            if (panelShapesSurface.getBorder() instanceof TitledBorder) {
                                ((TitledBorder) panelShapesSurface.getBorder()).setTitle("Surface Shapes (" + control.getSurface().getTriangleMids().length + ")");
                            }
                        }
//                            System.out.println("stop is showing? " + panelLoadingStatusStop.isShowing() + "  visible? " + panelLoadingStatusStop.isVisible() + "  is valid? " + panelLoadingStatusStop.isValid());
                        if (!panelLoadingStatusStop.isShowing()) {
                            panelLoadingStatus.removeAll();
                            panelLoadingStatus.add(panelLoadingStatusStop);
                        }

                        updatePanelInjectuions();

                        if (control.getLoadingCoordinator().isLoading()) {
                            if (currentAction != null) {
                                labelCurrentAction.setText(currentAction.toString());
                                progressLoading.setIndeterminate(!currentAction.hasProgress);
                                if (currentAction.hasProgress) {
                                    progressLoading.setValue((int) (currentAction.progress * 100));
                                }
                            } else {
                                labelCurrentAction.setText("No Action. Still Loading.");
                                progressLoading.setIndeterminate(true);
                            }
                            panelLoadingStatus.revalidate();
                            revalidate();
                            try {
                                sleep(updateThreadUpdateIntervalMS);
                            } catch (Exception e) {

                            }
                        } else {
//                            System.out.println("Park update thread");
                            //Reset the layout to non-working style
                            progressLoading.setIndeterminate(false);
                            progressLoading.setValue(0);
                            progressLoading.setStringPainted(false);
                            labelCurrentAction.setText("");
                            panelLoadingStatus.removeAll();
                            panelLoadingStatus.add(panelLoadingStatusStart);
                            revalidate();
                            synchronized (updatethreadBarrier) {
                                updatethreadBarrier.wait();
//                                System.out.println("update thread revoken");
                            }
                        }

                        panelLoadingStatusStart.revalidate();
                        panelLoadingStatusStop.revalidate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Update Thread is interrupted and terminates here.");
            }
        };
        updateThread.start();
    }

    private void updatePanelInjectuions() {
        //Update Injections Information
        if (control.getLoadingCoordinator() == null || control.getLoadingCoordinator().getInjections() == null) {
//                            System.out.println("remove Injections");
            panelInjections.removeAll();
        }

        if (control.getLoadingCoordinator() != null && control.getLoadingCoordinator().getInjections() != null) {

            if (control.getLoadingCoordinator().getInjections().size() != panelInjections.getComponentCount() - 1) {

                if (panelInjections.getBorder() != null && panelInjections.getBorder() instanceof TitledBorder) {
                    ((TitledBorder) panelInjections.getBorder()).setTitle(control.getLoadingCoordinator().getInjections().size() + " Injections");
                }
//                                System.out.println("new size: injections: " + (control.getLoadingCoordinator().getInjections().size()) + "  componennts: " + panelInjections.getComponentCount());
                panelInjections.removeAll();
                ArrayList<InjectionInformation> injections = control.getLoadingCoordinator().getInjections();
                if (injections.isEmpty()) {
                    panelInjections.setLayout(new BorderLayout());
                    panelInjections.add(new JLabel("No Injections"), BorderLayout.NORTH);
                } else {
                    BoxLayout layout = new BoxLayout(panelInjections, BoxLayout.Y_AXIS);
                    panelInjections.setLayout(layout);//new GridLayout(injections.size()+1, 1));
                    try {
                        for (final InjectionInformation inj : injections) {
                            InjectionPanel ip = new InjectionPanel(inj, mapViewer, paintManager);
                            panelInjections.add(ip);

                            //Create popup to delete this injection 
                            JPopupMenu popup = new JPopupMenu();
                            JMenuItem itemdelete = new JMenuItem("Delete from list");
                            popup.add(itemdelete);
                            itemdelete.addActionListener(new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent ae) {
                                    control.getLoadingCoordinator().getInjections().remove(inj);
                                    control.recalculateInjections();
                                    SingleControllPanel.this.updateGUI();
                                }
                            });
                            ip.setComponentPopupMenu(popup);
                        }
                    } catch (Exception e) {
                    }

                }
            }
        } else {
            panelInjections.setLayout(new BorderLayout());
            panelInjections.add(new JLabel("Injections = null"));

        }

        panelInjections.revalidate();
    }

}
