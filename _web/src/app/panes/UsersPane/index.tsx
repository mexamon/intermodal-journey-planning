import React, { useState, useMemo } from 'react';
import * as styles from '../Panes.module.scss';
import { mockUsers as initialUsers, mockTeams as initialTeams, mockPendingInvites } from '../../data/mockData';
import { User, Team } from '../../types';
import * as Tooltip from '@radix-ui/react-tooltip';
import * as Popover from '@radix-ui/react-popover';
import * as Dialog from '@radix-ui/react-dialog';
import {
    FiCheckCircle, FiXCircle, FiUserCheck, FiUserX, FiSettings, FiTrash2,
    FiPlus, FiX, FiSearch, FiCheck
} from 'react-icons/fi';

// Yeni bileşen: Takım Etiketi
const TeamTag = ({ team, onRemove }: { team: Team; onRemove: () => void; }) => (
    <div className={styles.teamTag} style={{ backgroundColor: `${team.color}20`, color: team.color }}>
        <span className={styles.teamTagDot} style={{ backgroundColor: team.color }}></span>
        {team.name}
        <button onClick={onRemove} className={styles.teamTagRemove}>
            <FiX size={12} />
        </button>
    </div>
);

const TeamAssignmentPopover = ({ user, allTeams, onAssign, onCreate }: {
    user: User,
    allTeams: Team[],
    onAssign: (teamId: string) => void,
    onCreate: () => void,
}) => {
    const [searchTerm, setSearchTerm] = useState('');
    const userTeamIds = new Set(user.teams);

    const filteredTeams = allTeams.filter(team =>
        team.name.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <Popover.Root>
            <Popover.Trigger asChild>
                <button className={`${styles.iconButton} ${styles.primaryOutline}`}>
                    <FiPlus />
                </button>
            </Popover.Trigger>
            <Popover.Portal>
                <Popover.Content className={styles.popoverContent} sideOffset={5}>
                    <div className={styles.popoverSearchWrapper}>
                        <FiSearch />
                        <input
                            type="text"
                            placeholder="Search teams..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className={styles.popoverSearchInput}
                        />
                    </div>
                    <div className={styles.popoverList}>
                        {filteredTeams.map(team => {
                            const isAssigned = userTeamIds.has(team.id);
                            return (
                                <div
                                    key={team.id}
                                    className={`${styles.popoverItem} ${isAssigned ? styles.disabled : ''}`}
                                    onClick={() => !isAssigned && onAssign(team.id)}
                                >
                                    <span>{team.name}</span>
                                    {isAssigned && <FiCheck />}
                                </div>
                            );
                        })}
                    </div>
                    <button className={styles.popoverCreateButton} onClick={onCreate}>
                        <FiPlus /> Create new team
                    </button>
                    {/* DÜZELTME: className prop'u buradan kaldırıldı */}
                    <Popover.Arrow />
                </Popover.Content>
            </Popover.Portal>
        </Popover.Root>
    );
};

export const UsersPane = () => {
    const [userTab, setUserTab] = useState('users');
    const [users, setUsers] = useState<User[]>(initialUsers);
    const [teams, setTeams] = useState<Team[]>(initialTeams);
    const [isModalOpen, setModalOpen] = useState(false);
    const [newTeamName, setNewTeamName] = useState('');

    // YENİ: Arama ve filtreleme için state'ler
    const [searchTerm, setSearchTerm] = useState('');
    const [roleFilter, setRoleFilter] = useState<'all' | 'admin' | 'member' | 'guest'>('all');

    // Filtrelenmiş kullanıcı listesini oluşturan mantık
    const filteredUsers = useMemo(() => {
        return users.filter(user => {
            const matchesSearch = user.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                                  user.email.toLowerCase().includes(searchTerm.toLowerCase());

            const matchesRole = roleFilter === 'all' ||
                                (roleFilter === 'admin' && user.role === 'Workspace admin') ||
                                (roleFilter === 'member' && user.role === 'Member') ||
                                (roleFilter === 'guest' && user.role === 'Guest');

            return matchesSearch && matchesRole;
        });
    }, [users, searchTerm, roleFilter]);

    const handleAssignTeam = (userId: string, teamId: string) => {
        setUsers(users.map(user =>
            user.id === userId ? { ...user, teams: [...user.teams, teamId] } : user
        ));
    };

    const handleRemoveTeam = (userId: string, teamId: string) => {
        setUsers(users.map(user =>
            user.id === userId ? { ...user, teams: user.teams.filter(id => id !== teamId) } : user
        ));
    };

    const handleCreateTeam = () => {
        if (!newTeamName.trim()) return;

        const newTeam: Team = {
            id: `team_${Date.now()}`,
            name: newTeamName,
            color: `#${Math.floor(Math.random() * 16777215).toString(16).padStart(6, '0')}`,
        };

        setTeams([...teams, newTeam]);
        setNewTeamName('');
        setModalOpen(false);
    };

    return (
        <>
            <header className={styles.paneHeader}>
                <h2>Workspace Users</h2>
                <p>Manage who has access to this workspace and their teams.</p>
            </header>

            <div className={styles.usersSummary}>
                <span><strong>{users.length}</strong> total user(s)</span>
                <span><strong>{users.filter(u => u.role === 'Workspace admin').length}</strong> Admin(s)</span>
                <span><strong>{users.filter(u => u.role === 'Member').length}</strong> Member(s)</span>
                <span><strong>{users.filter(u => u.role === 'Guest').length}</strong> Guest(s)</span>
            </div>
            <div className={styles.subTabs}>
                <button onClick={() => setUserTab('users')} className={userTab === 'users' ? styles.active : ''}>Users</button>
                <button onClick={() => setUserTab('pending')} className={userTab === 'pending' ? styles.active : ''}>Pending invitations ({mockPendingInvites.length})</button>
            </div>
            
            {/* YENİ: Arama ve Filtreleme Arayüzü */}
            <div className={styles.usersActions}>
                <div className={styles.userSearchAndFilter}>
                    <div className={styles.userSearchWrapper}>
                        <FiSearch />
                        <input 
                            type="text"
                            placeholder="Search by name or email..."
                            value={searchTerm}
                            onChange={e => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <div className={styles.userFilterButtons}>
                        <button onClick={() => setRoleFilter('all')} className={roleFilter === 'all' ? styles.active : ''}>All</button>
                        <button onClick={() => setRoleFilter('admin')} className={roleFilter === 'admin' ? styles.active : ''}>Admins</button>
                        <button onClick={() => setRoleFilter('member')} className={roleFilter === 'member' ? styles.active : ''}>Members</button>
                        <button onClick={() => setRoleFilter('guest')} className={roleFilter === 'guest' ? styles.active : ''}>Guests</button>
                    </div>
                </div>
                <div className={styles.actionButtonsGroup}>
                    <button className={`${styles.actionButton} ${styles.primary}`}>Invite people</button>
                </div>
            </div>

            {userTab === 'users' && (
                <div className={styles.tableWrapper}>
                    <table className={styles.dataTable}>
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Teams</th>
                                <th>Permissions</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {users.map(user => {
                                const userTeams = user.teams.map(teamId => teams.find(t => t.id === teamId)).filter(Boolean) as Team[];
                                return (
                                    <tr key={user.id}>
                                        <td>
                                            <div className={styles.userNameCell}>
                                                <img src={user.avatarUrl} alt={user.name} />
                                                <div><p>{user.name}</p><span>{user.email}</span></div>
                                            </div>
                                        </td>
                                        <td>
                                            <div className={styles.teamsCell}>
                                                {userTeams.map(team => (
                                                    <TeamTag key={team.id} team={team} onRemove={() => handleRemoveTeam(user.id, team.id)} />
                                                ))}
                                                <TeamAssignmentPopover
                                                    user={user}
                                                    allTeams={teams}
                                                    onAssign={(teamId) => handleAssignTeam(user.id, teamId)}
                                                    onCreate={() => setModalOpen(true)}
                                                />
                                            </div>
                                        </td>
                                        <td>
                                            <div className={styles.permissionsCell}>
                                                <Tooltip.Provider delayDuration={100}>
                                                    <Tooltip.Root>
                                                        <Tooltip.Trigger asChild>{user.permissions.createRooms ? <FiCheckCircle className={styles.permEnabled} /> : <FiXCircle className={styles.permDisabled} />}</Tooltip.Trigger>
                                                        <Tooltip.Portal><Tooltip.Content className={styles.tooltipContent}>Create rooms</Tooltip.Content></Tooltip.Portal>
                                                    </Tooltip.Root>
                                                    <Tooltip.Root>
                                                        <Tooltip.Trigger asChild>{user.permissions.publishTemplates ? <FiCheckCircle className={styles.permEnabled} /> : <FiXCircle className={styles.permDisabled} />}</Tooltip.Trigger>
                                                        <Tooltip.Portal><Tooltip.Content className={styles.tooltipContent}>Publish templates</Tooltip.Content></Tooltip.Portal>
                                                    </Tooltip.Root>
                                                    <Tooltip.Root>
                                                        <Tooltip.Trigger asChild>{user.permissions.seeOpenRooms ? <FiCheckCircle className={styles.permEnabled} /> : <FiXCircle className={styles.permDisabled} />}</Tooltip.Trigger>
                                                        <Tooltip.Portal><Tooltip.Content className={styles.tooltipContent}>See open rooms</Tooltip.Content></Tooltip.Portal>
                                                    </Tooltip.Root>
                                                    <Tooltip.Root>
                                                        <Tooltip.Trigger asChild>{user.permissions.isAdmin ? <FiUserCheck className={styles.permEnabled} /> : <FiUserX className={styles.permDisabled} />}</Tooltip.Trigger>
                                                        <Tooltip.Portal><Tooltip.Content className={styles.tooltipContent}>Workspace admin</Tooltip.Content></Tooltip.Portal>
                                                    </Tooltip.Root>
                                                </Tooltip.Provider>
                                            </div>
                                        </td>
                                        <td>
                                            <div className={styles.actionsCell}>
                                                <Tooltip.Provider delayDuration={100}>
                                                    <Tooltip.Root>
                                                        <Tooltip.Trigger className={styles.iconButton}><FiSettings /></Tooltip.Trigger>
                                                        <Tooltip.Portal><Tooltip.Content className={styles.tooltipContent}>Edit permissions</Tooltip.Content></Tooltip.Portal>
                                                    </Tooltip.Root>
                                                    <Tooltip.Root>
                                                        <Tooltip.Trigger className={`${styles.iconButton} ${styles.danger}`}><FiTrash2 /></Tooltip.Trigger>
                                                        <Tooltip.Portal><Tooltip.Content className={styles.tooltipContent}>Deactivate account</Tooltip.Content></Tooltip.Portal>
                                                    </Tooltip.Root>
                                                </Tooltip.Provider>
                                            </div>
                                        </td>
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                </div>
            )}
            
            {/* ====== EKSİK OLAN KISIM BURAYA EKLENDİ ====== */}
            {userTab === 'pending' && (
                <div className={styles.tableWrapper}>
                    <table className={styles.dataTable}>
                        <thead>
                            <tr>
                                <th>Email</th>
                                <th>Invited By</th>
                                <th>Date</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {mockPendingInvites.map(invite => (
                                <tr key={invite.id}>
                                    <td>{invite.email}</td>
                                    <td>{invite.invitedBy}</td>
                                    <td>{invite.date}</td>
                                    <td>
                                        <div className={styles.actionsCell}>
                                            <button className={`${styles.actionButton} ${styles.small}`}>Resend</button>
                                            <button className={`${styles.actionButton} ${styles.small} ${styles.danger}`}>Revoke</button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            {/* ====== EKSİK KISIM SONU ====== */}

           <Dialog.Root open={isModalOpen} onOpenChange={setModalOpen}>
                <Dialog.Portal>
                    <Dialog.Overlay className={styles.dialogOverlay} />
                    <Dialog.Content className={styles.dialogContent}>
                        <Dialog.Title className={styles.dialogTitle}>Create New Team</Dialog.Title>
                        
                        {/* YENİ: Kapatma butonu eklendi */}
                        <Dialog.Close asChild>
                          <button className={styles.dialogCloseButton} aria-label="Close">
                            <FiX />
                          </button>
                        </Dialog.Close>

                        <div className={styles.formGroup}>
                            <label htmlFor="team-name">Team Name</label>
                            <input
                                id="team-name"
                                className={styles.formInput}
                                value={newTeamName}
                                onChange={e => setNewTeamName(e.target.value)}
                                placeholder="e.g., Frontend Wizards"
                            />
                        </div>
                        <div style={{ display: 'flex', marginTop: 25, justifyContent: 'flex-end', gap: '1rem' }}>
                            <Dialog.Close asChild>
                                <button className={styles.actionButton}>Cancel</button>
                            </Dialog.Close>
                            <button className={`${styles.actionButton} ${styles.primary}`} onClick={handleCreateTeam}>Create</button>
                        </div>
                    </Dialog.Content>
                </Dialog.Portal>
            </Dialog.Root>
        </>
    );
};