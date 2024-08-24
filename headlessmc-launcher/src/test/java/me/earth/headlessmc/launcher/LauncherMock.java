package me.earth.headlessmc.launcher;

import lombok.experimental.UtilityClass;
import lombok.val;
import me.earth.headlessmc.api.HeadlessMcImpl;
import me.earth.headlessmc.api.command.line.CommandLine;
import me.earth.headlessmc.api.config.ConfigImpl;
import me.earth.headlessmc.api.config.HasConfig;
import me.earth.headlessmc.api.exit.ExitManager;
import me.earth.headlessmc.launcher.auth.*;
import me.earth.headlessmc.launcher.download.ChecksumService;
import me.earth.headlessmc.launcher.download.DownloadService;
import me.earth.headlessmc.launcher.download.MockDownloadService;
import me.earth.headlessmc.launcher.files.ConfigService;
import me.earth.headlessmc.launcher.files.FileManager;
import me.earth.headlessmc.launcher.java.JavaService;
import me.earth.headlessmc.launcher.launch.MockProcessFactory;
import me.earth.headlessmc.launcher.os.OS;
import me.earth.headlessmc.launcher.plugin.PluginManager;
import me.earth.headlessmc.launcher.specifics.VersionSpecificModManager;
import me.earth.headlessmc.launcher.version.VersionService;
import me.earth.headlessmc.logging.LoggingService;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class LauncherMock {
    public static final Launcher INSTANCE;

    static {
        INSTANCE = create();
    }

    public static Launcher create() {
        val base = FileManager.forPath("build");
        val fileManager = base.createRelative("fileManager");
        val configs = new ConfigService(fileManager);
        val in = new CommandLine();
        LoggingService loggingService = new LoggingService();
        val hmc = new HeadlessMcImpl(configs, in, new ExitManager(), loggingService);

        val os = new OS("windows", OS.Type.WINDOWS, "11", true);
        val mcFiles = base.createRelative("mcFiles");
        val versions = new VersionService(mcFiles);
        val javas = new JavaService(configs);

        val store = new DummyAccountStore(fileManager, configs);
        val accounts = new DummyAccountManager(store, new DummyAccountValidator());

        DownloadService downloadService = new MockDownloadService();
        val versionSpecificModManager = new VersionSpecificModManager(downloadService, fileManager.createRelative("specifics"));
        Launcher launcher = new Launcher(hmc, versions, mcFiles, mcFiles,
                new ChecksumService(), new MockDownloadService(),
                fileManager, new MockProcessFactory(downloadService, mcFiles, configs, os), configs,
                javas, accounts, versionSpecificModManager, new PluginManager());

        launcher.getConfigService().setConfig(ConfigImpl.empty());

        return launcher;
    }

    private static final class DummyAccountManager extends AccountManager {
        public DummyAccountManager(AccountStore accountStore, AccountValidator validator) {
            super(validator, new TestOfflineChecker(), accountStore);
        }
    }

    public static final class DummyAccountValidator extends AccountValidator {
        public static final String DUMMY_XUID = "dummy-xuid";

        @Override
        public ValidatedAccount validate(StepFullJavaSession.FullJavaSession session) {
            return new ValidatedAccount(session, DUMMY_XUID);
        }
    }

    public static final class DummyAccountStore extends AccountStore {
        public DummyAccountStore(FileManager fileManager, HasConfig cfg) {
            super(fileManager, cfg);
        }

        @Override
        public List<ValidatedAccount> load() {
            return new ArrayList<>();
        }

        @Override
        public void save(List<ValidatedAccount> accounts) {
            // NOP
        }
    }

}
